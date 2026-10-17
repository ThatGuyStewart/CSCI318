package com.retail.order.agentic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.retail.order.service.OrderProjectionRebuilder;

@Component
@ConditionalOnProperty(name = "retail.projection.replay.enabled", havingValue = "true")
public class OrderProjectionReplayRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderProjectionReplayRunner.class);

    private final OrderProjectionRebuilder projectionRebuilder;

    public OrderProjectionReplayRunner(OrderProjectionRebuilder projectionRebuilder) {
        this.projectionRebuilder = projectionRebuilder;
    }

    @Override
    public void run(String... args) {
        int eventsReplayed = projectionRebuilder.rebuild();
        log.info("Rebuilt OrderSummaryView from {} persisted Order events", eventsReplayed);
    }
}
