package com.retail.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BasketRemoveRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Positive
    private Integer quantity;

    public BasketRemoveRequest() {
    }

    public BasketRemoveRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
