package com.retail.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.notification.domain.NotificationView;

public interface NotificationViewRepository extends JpaRepository<NotificationView, Long> {
    List<NotificationView> findByCustomerId(Long customerId);
    List<NotificationView> findBySentBetween(LocalDateTime start, LocalDateTime end);
}