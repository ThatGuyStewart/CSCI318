package com.retail.order.dto;

public record OrderItemEventPayload(
        Long productId,
        String name,
        Double price,
        Integer quantity,
        Double subtotal) {
}