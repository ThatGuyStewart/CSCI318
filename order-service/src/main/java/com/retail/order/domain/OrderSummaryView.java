package com.retail.order.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_summary_views")
public class OrderSummaryView {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Double total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Embedded
    private Address address;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_summary_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_summary_product_ids", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "product_id")
    private List<Long> productIds = new ArrayList<>();

    public OrderSummaryView() {
    }

    public OrderSummaryView(Long id, Long customerId, Double total, OrderStatus status, LocalDateTime createdAt,
            Address address, List<OrderItem> items) {
        this.id = id;
        this.customerId = customerId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
        this.address = address;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.productIds = this.items.stream().map(OrderItem::getProductId).toList();
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Double getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Address getAddress() { return address; }
    public List<OrderItem> getItems() { return items; }
    public List<Long> getProductIds() { return productIds; }
}