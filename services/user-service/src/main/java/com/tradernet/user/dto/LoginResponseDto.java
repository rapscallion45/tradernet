package com.tradernet.user.dto;

/**
 * Login response payload.
 */
public class LoginResponseDto {

    private LoginStatus status;

    public LoginResponseDto() {
    }

    public LoginResponseDto(LoginStatus status) {
        this.status = status;
    }

    public LoginStatus getStatus() {
        return status;
    }

    public void setStatus(LoginStatus status) {
        this.status = status;
    }

}
