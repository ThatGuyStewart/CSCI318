package com.retail.notification.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.notification.domain.NotificationDomainEvent;

public interface NotificationDomainEventRepository extends JpaRepository<NotificationDomainEvent, UUID> {
}