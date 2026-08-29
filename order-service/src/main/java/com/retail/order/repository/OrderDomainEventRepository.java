package com.retail.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.retail.order.domain.OrderDomainEvent;

public interface OrderDomainEventRepository extends JpaRepository<OrderDomainEvent, UUID> {

    @Query("select coalesce(max(event.aggregateVersion), 0) from OrderDomainEvent event "
            + "where event.aggregateType = :aggregateType and event.aggregateId = :aggregateId")
    long findMaxAggregateVersion(String aggregateType, Long aggregateId);

    boolean existsByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);

    List<OrderDomainEvent> findByAggregateTypeOrderByOccurredAtAscAggregateVersionAsc(String aggregateType);

    List<OrderDomainEvent> findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc(
            String aggregateType, Long aggregateId);

        List<OrderDomainEvent> findByAggregateTypeAndAggregateIdInOrderByOccurredAtAscAggregateVersionAsc(
            String aggregateType, Set<Long> aggregateIds);

        List<OrderDomainEvent> findByAggregateTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
            String aggregateType, Instant start, Instant end);

        List<OrderDomainEvent> findByAggregateTypeAndAggregateIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByAggregateVersionAsc(
            String aggregateType, Long aggregateId, Instant start, Instant end);

        List<OrderDomainEvent> findByAggregateTypeAndAggregateIdInAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
            String aggregateType, Set<Long> aggregateIds, Instant start, Instant end);
}