package com.retail.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.retail.order.domain.OrderSummaryView;

public interface OrderSummaryViewRepository extends JpaRepository<OrderSummaryView, Long> {
    List<OrderSummaryView> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = "items")
    @Query("SELECT o FROM OrderSummaryView o WHERE o.id = :orderId")
    Optional<OrderSummaryView> findByIdWithItems(@Param("orderId") Long orderId);

    @EntityGraph(attributePaths = "items")
    @Query("SELECT o FROM OrderSummaryView o WHERE o.customerId = :customerId")
    List<OrderSummaryView> findByCustomerIdWithItems(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT o FROM OrderSummaryView o JOIN o.productIds productId WHERE productId = :productId")
    List<OrderSummaryView> findByProductId(@Param("productId") Long productId);

    @EntityGraph(attributePaths = "items")
    @Query("SELECT DISTINCT o FROM OrderSummaryView o JOIN o.productIds productId WHERE productId = :productId")
    List<OrderSummaryView> findByProductIdWithItems(@Param("productId") Long productId);
}