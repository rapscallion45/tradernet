package com.tradernet.api.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradernet.marketai.MarketDataViewService;
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
import java.util.concurrent.TimeUnit;

/**
 * Delivers market events through bounded per-session queues outside the ingestion callback.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MarketStreamDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(MarketStreamDeliveryService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_MAX_PENDING_EVENTS = 128;
    private static final long SEND_TIMEOUT_MS = 5_000L;

    @EJB
    private MarketDataViewService marketDataViewService;

    @Resource
    private SessionContext sessionContext;

    private final Map<String, ClientChannel> channels = new ConcurrentHashMap<>();

    public void register(Session session) {
        if (session == null || !session.isOpen()) {
            return;
        }
        session.getAsyncRemote().setSendTimeout(SEND_TIMEOUT_MS);
        final ClientChannel previous = channels.put(
            session.getId(),
            new ClientChannel(session, maxPendingEvents())
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

        while (true) {
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
                channel.session.getAsyncRemote().sendText(message).get(SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                failChannel(sessionId, channel, ex);
                return;
            } catch (JsonProcessingException ex) {
                LOG.warn("Unable to serialize market event for websocket session {}.", sessionId, ex);
            } catch (Exception ex) {
                failChannel(sessionId, channel, ex);
                return;
            }
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
        if (!channel.offer(event)) {
            return;
        }

        try {
            sessionContext.getBusinessObject(MarketStreamDeliveryService.class).drain(session.getId());
        } catch (RuntimeException ex) {
            channel.cancelDrain();
            failChannel(session.getId(), channel, ex);
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

    private int maxPendingEvents() {
        final String configured = System.getProperty(
            "market.ai.websocket.maxPendingEvents",
            String.valueOf(DEFAULT_MAX_PENDING_EVENTS)
        );
        try {
            return Math.max(1, Integer.parseInt(configured));
        } catch (NumberFormatException ex) {
            return DEFAULT_MAX_PENDING_EVENTS;
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

        private synchronized boolean offer(PendingEvent event) {
            if (closed) {
                return false;
            }
            while (pending.size() >= capacity) {
                pending.removeFirst();
            }
            pending.addLast(event);
            if (draining) {
                return false;
            }
            draining = true;
            return true;
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
