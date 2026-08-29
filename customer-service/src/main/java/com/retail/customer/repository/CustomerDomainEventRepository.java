package com.retail.customer.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.retail.customer.domain.CustomerDomainEvent;

public interface CustomerDomainEventRepository extends JpaRepository<CustomerDomainEvent, UUID> {

    @Query("select coalesce(max(event.aggregateVersion), 0) from CustomerDomainEvent event "
            + "where event.aggregateType = :aggregateType and event.aggregateId = :aggregateId")
    long findMaxAggregateVersion(String aggregateType, Long aggregateId);

        boolean existsByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);

        Optional<CustomerDomainEvent> findFirstByAggregateTypeAndCustomerEmailOrderByOccurredAtDescAggregateVersionDesc(
            String aggregateType, String customerEmail);

    List<CustomerDomainEvent> findByAggregateTypeOrderByOccurredAtAscAggregateVersionAsc(String aggregateType);

    List<CustomerDomainEvent> findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc(
            String aggregateType, Long aggregateId);

    List<CustomerDomainEvent> findByAggregateTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
            String aggregateType, Instant start, Instant end);

    List<CustomerDomainEvent> findByAggregateTypeAndAggregateIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByAggregateVersionAsc(
            String aggregateType, Long aggregateId, Instant start, Instant end);
}