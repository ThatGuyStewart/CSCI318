package com.retail.customer.dto;

import java.util.ArrayList;
import java.util.List;

public class BasketResponse {
    private Long customerId;
    private List<BasketItemResponse> items = new ArrayList<>();
    private Double total = 0.0;

    public BasketResponse() {
    }

    public BasketResponse(Long customerId, List<BasketItemResponse> items, Double total) {
        this.customerId = customerId;
        this.items = items != null ? items : new ArrayList<>();
        this.total = total != null ? total : 0.0;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<BasketItemResponse> getItems() {
        return items;
    }

    public void setItems(List<BasketItemResponse> items) {
        this.items = items;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
