package com.retail.customer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.retail.customer.domain.Basket;

public interface BasketRepository extends JpaRepository<Basket, Long> {

	@Query("select distinct basket from Basket basket join basket.items item where item.productId = :productId")
	List<Basket> findByItemsProductId(@Param("productId") Long productId);
}
