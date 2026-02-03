package com.tradernet.user.dto;

/**
 * Forgot password request payload.
 */
public class ForgotPasswordRequestDto {

    private String username;
    private String newPassword;

    public ForgotPasswordRequestDto() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
