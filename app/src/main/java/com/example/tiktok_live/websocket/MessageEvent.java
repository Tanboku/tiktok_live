package com.example.tiktok_live.websocket;

public class MessageEvent {
    private String message;

    public MessageEvent(String message) {
        this.message = message;
    }

    // Getter
    public String getMessage() {
        return message;
    }
}