package com.retail.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.notification.domain.NotificationProjectionCheckpoint;

public interface NotificationProjectionCheckpointRepository
        extends JpaRepository<NotificationProjectionCheckpoint, Long> {
}
