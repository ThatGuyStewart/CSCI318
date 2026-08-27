package com.retail.order.dto;

import java.util.List;

public class BasketDto {
    private Long customerId;
    private List<BasketItemDto> items;
    private Double total;

    public BasketDto() {
    }

    public BasketDto(Long customerId, List<BasketItemDto> items, Double total) {
        this.customerId = customerId;
        this.items = items;
        this.total = total;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<BasketItemDto> getItems() {
        return items;
    }

    public void setItems(List<BasketItemDto> items) {
        this.items = items;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
