package com.retail.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_projection_checkpoints")
public class CustomerProjectionCheckpoint {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false)
    private long aggregateVersion;

    protected CustomerProjectionCheckpoint() {
    }

    public CustomerProjectionCheckpoint(Long customerId, long aggregateVersion) {
        this.customerId = customerId;
        this.aggregateVersion = aggregateVersion;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }
}
