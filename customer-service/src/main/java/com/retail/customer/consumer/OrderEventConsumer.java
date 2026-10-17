package com.retail.customer.consumer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.Customer;
import com.retail.customer.domain.ProcessedOrderEvent;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerDomainEventRepository;
import com.retail.customer.repository.CustomerRepository;
import com.retail.customer.repository.ProcessedOrderEventRepository;
import com.retail.customer.service.CustomerEventRecorder;

@Component
@ConditionalOnProperty(name = "retail.events.consumer.enabled", havingValue = "true")
@SuppressWarnings("null")
public class OrderEventConsumer {

    private final BasketRepository basketRepository;
    private final ProcessedOrderEventRepository processedOrderEventRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDomainEventRepository customerDomainEventRepository;
    private final CustomerEventRecorder customerEventRecorder;

    public OrderEventConsumer(BasketRepository basketRepository,
            ProcessedOrderEventRepository processedOrderEventRepository, CustomerRepository customerRepository,
            CustomerDomainEventRepository customerDomainEventRepository, CustomerEventRecorder customerEventRecorder) {
        this.basketRepository = basketRepository;
        this.processedOrderEventRepository = processedOrderEventRepository;
        this.customerRepository = customerRepository;
        this.customerDomainEventRepository = customerDomainEventRepository;
        this.customerEventRecorder = customerEventRecorder;
    }

    @KafkaListener(topics = "${retail.events.order-topic}", groupId = "${retail.events.consumer.group-id}")
    @Transactional
    public void onOrderEvent(DomainEventMessage event) {
        if (!"BasketUsedForOrderEvent".equals(event.eventType())
                || processedOrderEventRepository.existsById(event.eventId())) {
            return;
        }

        Long customerId = asLong(event.payload());
        if (customerId == null) {
            return;
        }

        processedOrderEventRepository.save(new ProcessedOrderEvent(event.eventId()));
        basketRepository.findById(customerId).ifPresent(basket -> clearBasketAndRecord(basket, event));
    }

    private Long asLong(Map<String, Object> payload) {
        Object customerId = payload.get("customerId");
        return customerId instanceof Number number ? number.longValue() : null;
    }

    private void clearBasketAndRecord(Basket basket, DomainEventMessage orderEvent) {
        Customer customer = customerRepository.findByIdForUpdate(basket.getCustomerId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot record basket clearing for missing customer " + basket.getCustomerId()));
        if (!customer.hasAggregateVersion()) {
            customer.initializeAggregateVersion(customerDomainEventRepository.findMaxAggregateVersion(
                    "Customer", customer.getCustomerId()));
        }
        customer.nextAggregateVersion();
        customerRepository.save(customer);

        basket.clear();
        Basket saved = basketRepository.save(basket);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", saved.getCustomerId());
        payload.put("items", List.of());
        payload.put("total", saved.getTotal());
        customerEventRecorder.record(customer, "BasketClearedAfterOrderEvent", payload, orderEvent.eventId());
    }
}