package com.retail.customer.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.customer.domain.ProcessedProductEvent;

public interface ProcessedProductEventRepository extends JpaRepository<ProcessedProductEvent, UUID> {
}