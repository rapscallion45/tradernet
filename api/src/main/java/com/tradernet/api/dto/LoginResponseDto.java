package com.tradernet.api.dto;

/**
 * Login response payload.
 */
public class LoginResponseDto {

    private String token;
    private AuthUserDto user;

    public LoginResponseDto() {
    }

    public LoginResponseDto(String token, AuthUserDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AuthUserDto getUser() {
        return user;
    }

    public void setUser(AuthUserDto user) {
        this.user = user;
    }
}
