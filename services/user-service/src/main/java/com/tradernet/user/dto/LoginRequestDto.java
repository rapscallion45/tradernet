package com.tradernet.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request payload.
 */
public class LoginRequestDto {

    @NotBlank(message = "username is required")
    @Size(max = 50, message = "username must not exceed 50 characters")
    private String username;
    @NotBlank(message = "password is required")
    @Size(max = 512, message = "password input is too large")
    private String password;

    public LoginRequestDto() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
