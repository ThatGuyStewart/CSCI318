package com.retail.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

class DomainEventPublisherAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DomainEventPublisherAutoConfiguration.class));

    @Test
    void registersNoOpPublisherWhenPublishingIsDisabled() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(DomainEventPublisher.class));
    }

    @Test
    void registersKafkaPublisherWhenPublishingIsEnabled() {
        contextRunner.withPropertyValues("retail.events.publishing.enabled=true")
                .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                .run(context -> assertThat(context).hasSingleBean(DomainEventPublisher.class)
                        .getBean(DomainEventPublisher.class).isInstanceOf(KafkaDomainEventPublisher.class));
    }
}