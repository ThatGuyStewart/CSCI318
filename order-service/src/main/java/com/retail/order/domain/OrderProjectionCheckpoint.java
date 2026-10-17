package com.retail.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_projection_checkpoints")
public class OrderProjectionCheckpoint {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false)
    private long aggregateVersion;

    protected OrderProjectionCheckpoint() {
    }

    public OrderProjectionCheckpoint(Long orderId, long aggregateVersion) {
        this.orderId = orderId;
        this.aggregateVersion = aggregateVersion;
    }

    public Long getOrderId() {
        return orderId;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }
}
