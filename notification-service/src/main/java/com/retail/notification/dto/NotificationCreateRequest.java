package com.retail.notification.dto;

import jakarta.validation.constraints.NotBlank;

public class NotificationCreateRequest {
    @NotBlank
    private String message;

    public NotificationCreateRequest() {
    }

    public NotificationCreateRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
