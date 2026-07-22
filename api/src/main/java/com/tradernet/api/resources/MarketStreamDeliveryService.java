package com.tradernet.api.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.api.ApiConfiguration;
import com.tradernet.api.ApiObjectMapperProvider;
import com.tradernet.marketai.MarketDataViewProvider;
import com.tradernet.marketai.model.AiSignal;
import com.tradernet.marketai.model.MarketBar;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Delivers market events through bounded per-session queues outside the ingestion callback.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketStreamDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(MarketStreamDeliveryService.class);
    private static final ObjectMapper OBJECT_MAPPER = ApiObjectMapperProvider.createObjectMapper();
    private static final long SEND_TIMEOUT_MS = 5_000L;

    @EJB
    private MarketDataViewProvider marketDataViewService;

    @EJB
    private ApiConfiguration configuration;

    @Resource
    private SessionContext sessionContext;

    private final Map<String, ClientChannel> channels = new ConcurrentHashMap<>();
    private final LongAdder droppedEvents = new LongAdder();

    public void register(Session session) {
        if (session == null || !session.isOpen()) {
            return;
        }
        session.getAsyncRemote().setSendTimeout(SEND_TIMEOUT_MS);
        final ClientChannel previous = channels.put(
            session.getId(),
            new ClientChannel(session, configuration.getMaxPendingWebsocketEvents())
        );
        if (previous != null) {
            previous.close();
        }
    }

    public void unregister(Session session) {
        if (session == null) {
            return;
        }
        final ClientChannel channel = channels.remove(session.getId());
        if (channel != null) {
            channel.close();
        }
    }

    public void enqueueBar(Session session, MarketBar bar, String currency) {
        if (bar != null) {
            enqueue(session, PendingEvent.bar(bar, currency));
        }
    }

    public void enqueueSignal(Session session, AiSignal signal) {
        if (signal != null) {
            enqueue(session, PendingEvent.signal(signal));
        }
    }

    @Asynchronous
    public void drain(String sessionId) {
        final ClientChannel channel = channels.get(sessionId);
        if (channel == null) {
            return;
        }

        final PendingEvent event = channel.next();
        if (event == null) {
            return;
        }

        try {
            final Object payload = event.bar == null
                ? event.signal
                : marketDataViewService.convertBar(event.bar, event.currency);
            final String message = OBJECT_MAPPER.writeValueAsString(Map.of(
                "type", event.type,
                "payload", payload
            ));
            final MarketStreamDeliveryService service = sessionContext
                .getBusinessObject(MarketStreamDeliveryService.class);
            channel.session.getAsyncRemote().sendText(message, result -> {
                if (result.isOK()) {
                    service.drain(sessionId);
                } else {
                    service.failDelivery(sessionId, result.getException());
                }
            });
        } catch (JsonProcessingException ex) {
            LOG.warn("Unable to serialize market event for websocket session {}.", sessionId, ex);
            sessionContext.getBusinessObject(MarketStreamDeliveryService.class).drain(sessionId);
        } catch (RuntimeException ex) {
            failChannel(sessionId, channel, ex);
        }
    }

    @Asynchronous
    public void failDelivery(String sessionId, Throwable error) {
        final ClientChannel channel = channels.get(sessionId);
        if (channel != null) {
            final Exception failure = error instanceof Exception
                ? (Exception) error
                : new IllegalStateException("Websocket send failed", error);
            failChannel(sessionId, channel, failure);
        }
    }

    private void enqueue(Session session, PendingEvent event) {
        if (session == null || event == null || !session.isOpen()) {
            return;
        }

        final ClientChannel channel = channels.get(session.getId());
        if (channel == null) {
            return;
        }
        final EnqueueDecision decision = channel.offer(event);
        if (!decision.accepted) {
            return;
        }
        if (decision.droppedOldest) {
            recordDroppedEvent(session.getId());
        }
        if (!decision.scheduleDrain) {
            return;
        }

        try {
            sessionContext.getBusinessObject(MarketStreamDeliveryService.class).drain(session.getId());
        } catch (RuntimeException ex) {
            channel.cancelDrain();
            failChannel(session.getId(), channel, ex);
        }
    }

    private void recordDroppedEvent(String sessionId) {
        droppedEvents.increment();
        final long count = droppedEvents.sum();
        if ((count & (count - 1L)) == 0L) {
            LOG.warn("Dropped {} queued market websocket events; latest affected session was {}.", count, sessionId);
        }
    }

    private void failChannel(String sessionId, ClientChannel channel, Exception ex) {
        channels.remove(sessionId, channel);
        channel.close();
        LOG.warn("Stopped market delivery for websocket session {}.", sessionId, ex);
        try {
            if (channel.session.isOpen()) {
                channel.session.close(new CloseReason(
                    CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                    "Market stream delivery failed"
                ));
            }
        } catch (IOException ignored) {
            // The session is already unusable.
        }
    }

    private static final class ClientChannel {
        private final Session session;
        private final int capacity;
        private final ArrayDeque<PendingEvent> pending = new ArrayDeque<>();
        private boolean draining;
        private boolean closed;

        private ClientChannel(Session session, int capacity) {
            this.session = session;
            this.capacity = capacity;
        }

        private synchronized EnqueueDecision offer(PendingEvent event) {
            if (closed) {
                return EnqueueDecision.rejected();
            }
            boolean droppedOldest = false;
            while (pending.size() >= capacity) {
                pending.removeFirst();
                droppedOldest = true;
            }
            pending.addLast(event);
            if (draining) {
                return EnqueueDecision.accepted(false, droppedOldest);
            }
            draining = true;
            return EnqueueDecision.accepted(true, droppedOldest);
        }

        private synchronized PendingEvent next() {
            if (closed) {
                draining = false;
                return null;
            }
            final PendingEvent event = pending.pollFirst();
            if (event == null) {
                draining = false;
            }
            return event;
        }

        private synchronized void cancelDrain() {
            draining = false;
        }

        private synchronized void close() {
            closed = true;
            draining = false;
            pending.clear();
        }
    }

    private static final class EnqueueDecision {
        private final boolean accepted;
        private final boolean scheduleDrain;
        private final boolean droppedOldest;

        private EnqueueDecision(boolean accepted, boolean scheduleDrain, boolean droppedOldest) {
            this.accepted = accepted;
            this.scheduleDrain = scheduleDrain;
            this.droppedOldest = droppedOldest;
        }

        private static EnqueueDecision accepted(boolean scheduleDrain, boolean droppedOldest) {
            return new EnqueueDecision(true, scheduleDrain, droppedOldest);
        }

        private static EnqueueDecision rejected() {
            return new EnqueueDecision(false, false, false);
        }
    }

    private static final class PendingEvent {
        private final String type;
        private final MarketBar bar;
        private final AiSignal signal;
        private final String currency;

        private PendingEvent(String type, MarketBar bar, AiSignal signal, String currency) {
            this.type = type;
            this.bar = bar;
            this.signal = signal;
            this.currency = currency;
        }

        private static PendingEvent bar(MarketBar bar, String currency) {
            return new PendingEvent("bar", bar, null, currency);
        }

        private static PendingEvent signal(AiSignal signal) {
            return new PendingEvent("signal", null, signal, null);
        }
    }
}
