package com.retail.order.repository;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.retail.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select order from Order order where order.id = :id")
    java.util.Optional<Order> findByIdForUpdate(Long id);

    List<Order> findByCustomerId(Long customerId);

    @Query("SELECT o FROM Order o JOIN o.items i WHERE i.productId = :productId")
    List<Order> findByProductId(@Param("productId") Long productId);
}
