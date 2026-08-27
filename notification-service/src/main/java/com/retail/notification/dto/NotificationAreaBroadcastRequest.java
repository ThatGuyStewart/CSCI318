package com.retail.notification.dto;

import jakarta.validation.constraints.NotBlank;

public class NotificationAreaBroadcastRequest {
    @NotBlank
    private String message;

    private Integer postcode;
    private String state;
    private String country;

    public NotificationAreaBroadcastRequest() {
    }

    public NotificationAreaBroadcastRequest(String message, Integer postcode, String state, String country) {
        this.message = message;
        this.postcode = postcode;
        this.state = state;
        this.country = country;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getPostcode() {
        return postcode;
    }

    public void setPostcode(Integer postcode) {
        this.postcode = postcode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
