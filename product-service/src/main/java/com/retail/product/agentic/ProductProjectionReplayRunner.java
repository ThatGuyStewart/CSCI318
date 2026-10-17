package com.retail.product.agentic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.retail.product.service.ProductProjectionRebuilder;

@Component
@ConditionalOnProperty(name = "retail.projection.replay.enabled", havingValue = "true")
public class ProductProjectionReplayRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductProjectionReplayRunner.class);

    private final ProductProjectionRebuilder projectionRebuilder;

    public ProductProjectionReplayRunner(ProductProjectionRebuilder projectionRebuilder) {
        this.projectionRebuilder = projectionRebuilder;
    }

    @Override
    public void run(String... args) {
        int eventsReplayed = projectionRebuilder.rebuild();
        log.info("Rebuilt ProductView from {} persisted Product events", eventsReplayed);
    }
}
