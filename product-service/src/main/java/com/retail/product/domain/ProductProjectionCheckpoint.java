package com.retail.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_projection_checkpoints")
public class ProductProjectionCheckpoint {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private long aggregateVersion;

    protected ProductProjectionCheckpoint() {
    }

    public ProductProjectionCheckpoint(Long productId, long aggregateVersion) {
        this.productId = productId;
        this.aggregateVersion = aggregateVersion;
    }

    public Long getProductId() {
        return productId;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }
}
