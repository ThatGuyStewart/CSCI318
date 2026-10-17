package com.retail.notification.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.notification.domain.NotificationDomainEvent;

public interface NotificationDomainEventRepository extends JpaRepository<NotificationDomainEvent, UUID> {

    List<NotificationDomainEvent> findByAggregateTypeOrderByAggregateIdAscAggregateVersionAsc(String aggregateType);

    List<NotificationDomainEvent> findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc(
            String aggregateType, Long aggregateId);
}