package com.retail.order.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Embedded
    private Address address;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Double total = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.Placed;

    private Long aggregateVersion;

    public Order() {
    }

    public Order(Long customerId, Address address, List<OrderItem> items, Double total, OrderStatus status) {
        this.customerId = customerId;
        this.address = address;
        this.items = items != null ? items : new ArrayList<>();
        this.total = total != null ? total : 0.0;
        this.status = status != null ? status : OrderStatus.Placed;
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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
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

    public long nextAggregateVersion() {
        aggregateVersion = getAggregateVersion() + 1;
        return aggregateVersion;
    }

    public long getAggregateVersion() {
        return aggregateVersion != null ? aggregateVersion : 0;
    }

    public boolean hasAggregateVersion() {
        return aggregateVersion != null;
    }

    public void initializeAggregateVersion(long aggregateVersion) {
        if (this.aggregateVersion == null) {
            this.aggregateVersion = aggregateVersion;
        }
    }
}
