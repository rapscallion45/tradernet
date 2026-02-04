package com.tradernet.user.dto;

/**
 * Login response payload.
 */
public class LoginResponseDto {

    private AuthUserDto user;
    private boolean mustResetPassword;

    public LoginResponseDto() {
    }

    public LoginResponseDto(AuthUserDto user) {
        this.user = user;
    }

    public LoginResponseDto(AuthUserDto user, boolean mustResetPassword) {
        this.user = user;
        this.mustResetPassword = mustResetPassword;
    }

    public AuthUserDto getUser() {
        return user;
    }

    public void setUser(AuthUserDto user) {
        this.user = user;
    }

    public boolean isMustResetPassword() {
        return mustResetPassword;
    }

    public void setMustResetPassword(boolean mustResetPassword) {
        this.mustResetPassword = mustResetPassword;
    }
}
