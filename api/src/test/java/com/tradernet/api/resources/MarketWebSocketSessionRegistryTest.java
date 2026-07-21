package com.tradernet.api.resources;

import com.tradernet.user.AuthSessionService;
import com.tradernet.user.AuthenticationAuditService;
import com.tradernet.user.AuthorizationService;
import com.tradernet.user.dto.AuthUserDto;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarketWebSocketSessionRegistryTest {

    @Test
    void logoutImmediatelyClosesTheMatchingConnection() {
        final StubAuthSessionService sessions = new StubAuthSessionService();
        final MarketWebSocketSessionRegistry registry = registry(sessions);
        final AtomicReference<CloseReason> closeReason = new AtomicReference<>();
        final Session session = session("connection-1", closeReason);

        registry.register(session, "session-token", user());
        registry.closeBySessionToken("other-token");
        assertNull(closeReason.get());

        registry.closeBySessionToken("session-token");
        assertEquals("Session logged out", closeReason.get().getReasonPhrase());
    }

    @Test
    void scheduledValidationClosesExpiredSessions() {
        final StubAuthSessionService sessions = new StubAuthSessionService();
        sessions.currentUser = Optional.empty();
        final MarketWebSocketSessionRegistry registry = registry(sessions);
        final AtomicReference<CloseReason> closeReason = new AtomicReference<>();
        registry.register(session("connection-2", closeReason), "expired-token", user());

        registry.revalidateConnections();

        assertEquals("Session expired", closeReason.get().getReasonPhrase());
    }

    @Test
    void passwordResetImmediatelyClosesEveryConnectionForTheUser() {
        final MarketWebSocketSessionRegistry registry = registry(new StubAuthSessionService());
        final AtomicReference<CloseReason> firstCloseReason = new AtomicReference<>();
        final AtomicReference<CloseReason> secondCloseReason = new AtomicReference<>();
        registry.register(session("connection-3", firstCloseReason), "first-token", user());
        registry.register(session("connection-4", secondCloseReason), "second-token", user());

        registry.closeByUserId(1L);

        assertEquals("Credentials changed", firstCloseReason.get().getReasonPhrase());
        assertEquals("Credentials changed", secondCloseReason.get().getReasonPhrase());
    }

    private MarketWebSocketSessionRegistry registry(StubAuthSessionService sessions) {
        return new MarketWebSocketSessionRegistry(
            sessions,
            new StubAuthorizationService(),
            new NoOpAuditService()
        );
    }

    private AuthUserDto user() {
        return new AuthUserDto(1L, "alice", Set.of("Standard Rights"));
    }

    private Session session(String id, AtomicReference<CloseReason> closeReason) {
        return (Session) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{Session.class},
            (proxy, method, arguments) -> {
                if ("getId".equals(method.getName())) {
                    return id;
                }
                if ("close".equals(method.getName()) && arguments != null && arguments.length == 1) {
                    closeReason.set((CloseReason) arguments[0]);
                    return null;
                }
                if ("isOpen".equals(method.getName())) {
                    return closeReason.get() == null;
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(method.getName())) {
                    return proxy == arguments[0];
                }
                return null;
            }
        );
    }

    private static final class StubAuthSessionService extends AuthSessionService {
        private Optional<AuthUserDto> currentUser = Optional.of(
            new AuthUserDto(1L, "alice", Set.of("Standard Rights"))
        );

        @Override
        public Optional<AuthUserDto> validateSessionUser(String sessionId) {
            return currentUser;
        }
    }

    private static final class StubAuthorizationService extends AuthorizationService {
        @Override
        public Set<String> getRequiredRoles(String httpMethod, String path) {
            return Set.of("Standard Rights");
        }

        @Override
        public boolean hasAnyRole(AuthUserDto authUser, Set<String> allowedRoles) {
            return authUser.getRoleNames().stream().anyMatch(allowedRoles::contains);
        }
    }

    private static final class NoOpAuditService extends AuthenticationAuditService {
        @Override
        public void record(String event, String outcome, String subject, String sourceAddress, String reason) {
        }
    }
}
