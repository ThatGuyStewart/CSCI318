package com.retail.order.dto;

import com.retail.order.domain.OrderStatus;
import java.util.List;

public class OrderResponse {
    private Long id;
    private Long customerId;
    private AddressDto address;
    private List<OrderItemDto> items;
    private Double total;
    private OrderStatus status;

    public OrderResponse() {
    }

    public OrderResponse(Long id, Long customerId, AddressDto address, List<OrderItemDto> items, Double total, OrderStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.address = address;
        this.items = items;
        this.total = total;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
