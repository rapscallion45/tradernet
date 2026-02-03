package com.tradernet.user.dto;

import com.tradernet.jpa.entities.UserEntity;

/**
 * Lightweight auth user payload.
 */
public class AuthUserDto {

    private long id;
    private String username;

    public AuthUserDto() {
    }

    public AuthUserDto(long id, String username) {
        this.id = id;
        this.username = username;
    }

    public static AuthUserDto fromUser(UserEntity user) {
        return new AuthUserDto(user.getPk(), user.getUsername());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
