package com.tradernet.user;

import com.tradernet.user.dto.AuthUserDto;
import jakarta.ejb.Local;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Contract for authenticated and password-reset session workflows.
 */
@Local
public interface AuthSessionOperations {

    String createSession(AuthUserDto authUser);

    String createPasswordResetSession(long userId);

    Optional<AuthUserDto> getSessionUser(String sessionId);

    Optional<AuthUserDto> validateSessionUser(String sessionId);

    boolean hasValidSession(String sessionId);

    void removeSession(String sessionId);

    OptionalLong consumePasswordResetSession(String resetToken);

    void removeSessionsForUser(long userId);

    void removePasswordResetSessionForUser(long userId);

    void removePasswordResetSession(String resetToken);
}
