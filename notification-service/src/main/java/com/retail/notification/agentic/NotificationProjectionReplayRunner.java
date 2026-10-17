package com.retail.notification.agentic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.retail.notification.service.NotificationProjectionRebuilder;

@Component
@ConditionalOnProperty(name = "retail.projection.replay.enabled", havingValue = "true")
public class NotificationProjectionReplayRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationProjectionReplayRunner.class);

    private final NotificationProjectionRebuilder projectionRebuilder;

    public NotificationProjectionReplayRunner(NotificationProjectionRebuilder projectionRebuilder) {
        this.projectionRebuilder = projectionRebuilder;
    }

    @Override
    public void run(String... args) {
        int eventsReplayed = projectionRebuilder.rebuild();
        log.info("Rebuilt NotificationView from {} persisted Notification events", eventsReplayed);
    }
}
