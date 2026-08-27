package com.retail.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.customer.domain.Basket;

public interface BasketRepository extends JpaRepository<Basket, Long> {
}
