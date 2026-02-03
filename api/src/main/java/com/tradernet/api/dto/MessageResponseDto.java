package com.tradernet.api.dto;

/**
 * Simple message response payload.
 */
public class MessageResponseDto {

    private String message;

    public MessageResponseDto() {
    }

    public MessageResponseDto(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
