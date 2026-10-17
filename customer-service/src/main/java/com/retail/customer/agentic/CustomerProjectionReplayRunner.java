package com.retail.customer.agentic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.retail.customer.service.CustomerProjectionRebuilder;

@Component
@ConditionalOnProperty(name = "retail.projection.replay.enabled", havingValue = "true")
public class CustomerProjectionReplayRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CustomerProjectionReplayRunner.class);

    private final CustomerProjectionRebuilder projectionRebuilder;

    public CustomerProjectionReplayRunner(CustomerProjectionRebuilder projectionRebuilder) {
        this.projectionRebuilder = projectionRebuilder;
    }

    @Override
    public void run(String... args) {
        int eventsReplayed = projectionRebuilder.rebuild();
        log.info("Rebuilt CustomerView from {} persisted Customer events", eventsReplayed);
    }
}
