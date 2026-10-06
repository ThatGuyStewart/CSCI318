package com.retail.product.dto;

import java.util.List;

public class RecommendationResponse {

    private String sessionId;
    private String message;
    private List<ProductResponse> products;

    public RecommendationResponse() {
    }

    public RecommendationResponse(String sessionId, String message, List<ProductResponse> products) {
        this.sessionId = sessionId;
        this.message = message;
        this.products = products;
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

    public List<ProductResponse> getProducts() {
        return products;
    }

    public void setProducts(List<ProductResponse> products) {
        this.products = products;
    }
}
