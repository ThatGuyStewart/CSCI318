package com.retail.order.dto;

public class NotificationCreateRequest {
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
