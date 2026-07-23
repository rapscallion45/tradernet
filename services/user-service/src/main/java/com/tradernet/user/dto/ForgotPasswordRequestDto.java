package com.tradernet.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Forgot password request payload.
 */
public class ForgotPasswordRequestDto {

    @NotBlank(message = "newPassword is required")
    @Size(max = 512, message = "newPassword input is too large")
    private String newPassword;

    public ForgotPasswordRequestDto() {
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
