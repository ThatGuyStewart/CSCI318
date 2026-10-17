package com.retail.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.customer.domain.CustomerProjectionCheckpoint;

public interface CustomerProjectionCheckpointRepository
        extends JpaRepository<CustomerProjectionCheckpoint, Long> {
}
