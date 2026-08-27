package com.retail.order.dto;

import java.util.List;

public class OrderCreateRequest {
    private Long customerId;
    private AddressDto address;
    private List<OrderItemDto> items;

    public OrderCreateRequest() {
    }

    public OrderCreateRequest(Long customerId, AddressDto address, List<OrderItemDto> items) {
        this.customerId = customerId;
        this.address = address;
        this.items = items;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
