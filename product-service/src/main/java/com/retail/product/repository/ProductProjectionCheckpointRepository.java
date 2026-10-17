package com.retail.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.product.domain.ProductProjectionCheckpoint;

public interface ProductProjectionCheckpointRepository
        extends JpaRepository<ProductProjectionCheckpoint, Long> {
}
