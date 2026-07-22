package com.tradernet.user;

import jakarta.ejb.Local;

/**
 * API-facing authentication workflow contract.
 */
@Local
public interface AuthenticationOperations {

    AuthenticationResult login(String username, String password, String sourceAddress);

    PasswordResetResult resetPassword(String resetToken, String newPassword, String sourceAddress);

    void logout(String sessionId, String passwordResetToken, String sourceAddress);
}
