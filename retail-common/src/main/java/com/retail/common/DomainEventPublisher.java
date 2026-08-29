package com.retail.common;

public interface DomainEventPublisher {

    void publish(String topic, DomainEventMessage event);
}