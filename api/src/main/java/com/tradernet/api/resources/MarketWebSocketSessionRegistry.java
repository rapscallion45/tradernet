package com.tradernet.api.resources;

import com.tradernet.user.AuthSessionService;
import com.tradernet.user.AuthenticationAuditService;
import com.tradernet.user.AuthorizationService;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks market WebSockets so session expiry, authorization changes, and logout revoke live connections.
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class MarketWebSocketSessionRegistry {

    private static final String MARKET_PATH = "market";
    private static final String MARKET_METHOD = "GET";

    @EJB
    private AuthSessionService authSessionService;

    @EJB
    private AuthorizationService authorizationService;

    @EJB
    private AuthenticationAuditService auditService;

    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    public MarketWebSocketSessionRegistry() {
    }

    MarketWebSocketSessionRegistry(
        AuthSessionService authSessionService,
        AuthorizationService authorizationService,
        AuthenticationAuditService auditService
    ) {
        this.authSessionService = authSessionService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    public void register(Session session, String sessionToken, AuthUserDto authUser) {
        connections.put(session.getId(), new Connection(
            session,
            sessionToken,
            authUser.getId(),
            authUser.getUsername()
        ));
        auditService.record("websocket", "connected", authUser.getUsername(), null, "market");
    }

    public void unregister(Session session) {
        if (session == null) {
            return;
        }
        final Connection removed = connections.remove(session.getId());
        if (removed != null) {
            auditService.record("websocket", "disconnected", removed.username, null, "market");
        }
    }

    public void closeBySessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        connections.values().stream()
            .filter(connection -> sessionToken.equals(connection.sessionToken))
            .forEach(connection -> close(connection, "Session logged out"));
    }

    public void closeByUserId(long userId) {
        if (userId <= 0) {
            return;
        }
        connections.values().stream()
            .filter(connection -> userId == connection.userId)
            .forEach(connection -> close(connection, "Credentials changed"));
    }

    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    public void revalidateConnections() {
        final Set<String> requiredRoles = authorizationService.getRequiredRoles(MARKET_METHOD, MARKET_PATH);
        for (Connection connection : connections.values()) {
            final Optional<AuthUserDto> authUser = authSessionService.validateSessionUser(connection.sessionToken);
            if (authUser.isEmpty()) {
                close(connection, "Session expired");
            } else if (requiredRoles.isEmpty() || !authorizationService.hasAnyRole(authUser.get(), requiredRoles)) {
                close(connection, "Market access revoked");
            }
        }
    }

    private void close(Connection connection, String reason) {
        if (!connections.remove(connection.session.getId(), connection)) {
            return;
        }
        auditService.record("websocket", "revoked", connection.username, null, reason);
        try {
            connection.session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (IOException ignored) {
            // The connection is already absent from the registry.
        }
    }

    private static final class Connection {
        private final Session session;
        private final String sessionToken;
        private final long userId;
        private final String username;

        private Connection(Session session, String sessionToken, long userId, String username) {
            this.session = session;
            this.sessionToken = sessionToken;
            this.userId = userId;
            this.username = username;
        }
    }
}
