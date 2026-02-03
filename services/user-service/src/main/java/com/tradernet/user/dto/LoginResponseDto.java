package com.tradernet.user.dto;

/**
 * Login response payload.
 */
public class LoginResponseDto {

    private AuthUserDto user;

    public LoginResponseDto() {
    }

    public LoginResponseDto(AuthUserDto user) {
        this.user = user;
    }

    public AuthUserDto getUser() {
        return user;
    }

    public void setUser(AuthUserDto user) {
        this.user = user;
    }
}
