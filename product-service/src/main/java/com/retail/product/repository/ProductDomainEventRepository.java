package com.retail.product.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.retail.product.domain.ProductDomainEvent;
import com.retail.product.domain.ProductCategory;

public interface ProductDomainEventRepository extends JpaRepository<ProductDomainEvent, UUID> {

    @Query("select coalesce(max(event.aggregateVersion), 0) from ProductDomainEvent event "
            + "where event.aggregateType = :aggregateType and event.aggregateId = :aggregateId")
    long findMaxAggregateVersion(String aggregateType, Long aggregateId);

    boolean existsByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);

    List<ProductDomainEvent> findByAggregateTypeOrderByOccurredAtAscAggregateVersionAsc(String aggregateType);

    List<ProductDomainEvent> findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc(
            String aggregateType, Long aggregateId);

        List<ProductDomainEvent> findByAggregateTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
            String aggregateType, Instant start, Instant end);

        List<ProductDomainEvent> findByAggregateTypeAndAggregateIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByAggregateVersionAsc(
            String aggregateType, Long aggregateId, Instant start, Instant end);

            List<ProductDomainEvent> findByAggregateTypeAndEventCategoryOrderByOccurredAtAscAggregateVersionAsc(
                String aggregateType, ProductCategory eventCategory);

            List<ProductDomainEvent> findByAggregateTypeAndEventCategoryAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
                String aggregateType, ProductCategory eventCategory, Instant start, Instant end);
}