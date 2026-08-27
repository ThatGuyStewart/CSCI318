package com.retail.notification.dto;

import jakarta.validation.constraints.NotBlank;

public class NotificationBroadcastRequest {
    @NotBlank
    private String message;

    public NotificationBroadcastRequest() {
    }

    public NotificationBroadcastRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
