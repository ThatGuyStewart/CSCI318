package com.retail.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.order.domain.OrderProjectionCheckpoint;

public interface OrderProjectionCheckpointRepository extends JpaRepository<OrderProjectionCheckpoint, Long> {
}
