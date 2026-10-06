package com.retail.product.dto;

public class RecommendationRequest {

    private String sessionId;
    private String message;

    public RecommendationRequest() {
    }

    public RecommendationRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
