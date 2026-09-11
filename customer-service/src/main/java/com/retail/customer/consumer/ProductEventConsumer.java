package com.retail.customer.consumer;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.Customer;
import com.retail.customer.domain.CustomerDomainEvent;
import com.retail.customer.domain.ProcessedProductEvent;
import com.retail.customer.repository.CustomerDomainEventRepository;
import com.retail.customer.repository.CustomerRepository;
import com.retail.customer.repository.CustomerViewRepository;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.ProcessedProductEventRepository;

@Component
@ConditionalOnProperty(name = "retail.events.consumer.enabled", havingValue = "true")
@SuppressWarnings("null")
public class ProductEventConsumer {

    private final BasketRepository basketRepository;
    private final ProcessedProductEventRepository processedProductEventRepository;
    private final CustomerViewRepository customerViewRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDomainEventRepository customerDomainEventRepository;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final String eventTopic;

    public ProductEventConsumer(BasketRepository basketRepository,
            ProcessedProductEventRepository processedProductEventRepository, CustomerViewRepository customerViewRepository,
            CustomerRepository customerRepository, CustomerDomainEventRepository customerDomainEventRepository,
            ObjectMapper objectMapper, DomainEventPublisher domainEventPublisher,
            @Value("${retail.events.topic}") String eventTopic) {
        this.basketRepository = basketRepository;
        this.processedProductEventRepository = processedProductEventRepository;
        this.customerViewRepository = customerViewRepository;
        this.customerRepository = customerRepository;
        this.customerDomainEventRepository = customerDomainEventRepository;
        this.objectMapper = objectMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopic = eventTopic;
    }

    @KafkaListener(topics = "${retail.events.product-topic}", groupId = "${retail.events.consumer.group-id}")
    @Transactional
    public void onProductEvent(DomainEventMessage event) {
        if (!("ProductUpdatedEvent".equals(event.eventType()) || "ProductDeletedEvent".equals(event.eventType()))
                || processedProductEventRepository.existsById(event.eventId())) {
            return;
        }

        Map<String, Object> payload = event.payload();
        Long productId = asLong(payload.get("productId"));
        if (productId == null) {
            return;
        }

        processedProductEventRepository.save(new ProcessedProductEvent(event.eventId()));
        String name = asString(payload.get("name"));
        Double price = asDouble(payload.get("price"));

        for (Basket basket : basketRepository.findByItemsProductId(productId)) {
            boolean basketChanged = false;
            if ("ProductDeletedEvent".equals(event.eventType())) {
                basket.getItems().removeIf(item -> productId.equals(item.getProductId()));
                basketChanged = true;
            } else {
                for (var item : basket.getItems()) {
                    if (productId.equals(item.getProductId())) {
                        if (name != null && !Objects.equals(name, item.getName())) {
                            item.setName(name);
                            basketChanged = true;
                        }
                        if (price != null && !Objects.equals(price, item.getPrice())) {
                            item.setPrice(price);
                            basketChanged = true;
                        }
                    }
                }
            }
            if (!basketChanged) {
                continue;
            }
            basket.recalculateTotal();
            Basket saved = basketRepository.save(basket);
            customerViewRepository.findById(saved.getCustomerId()).ifPresent(view -> {
                view.setBasketTotal(saved.getTotal());
                customerViewRepository.save(view);
            });
            appendBasketRecalculatedEvent(saved);
        }
    }

    private void appendBasketRecalculatedEvent(Basket basket) {
        customerRepository.findById(basket.getCustomerId()).ifPresent(customer -> {
            initializeAggregateVersion(customer);
            customer.nextAggregateVersion();
            customerRepository.save(customer);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("customerId", basket.getCustomerId());
            payload.put("items", basket.getItems().stream().map(item -> {
                Map<String, Object> itemPayload = new LinkedHashMap<>();
                itemPayload.put("productId", item.getProductId());
                itemPayload.put("name", item.getName());
                itemPayload.put("price", item.getPrice());
                itemPayload.put("quantity", item.getQuantity());
                itemPayload.put("subtotal", item.getSubtotal());
                return itemPayload;
            }).toList());
            payload.put("total", basket.getTotal());
            try {
                CustomerDomainEvent recalculationEvent = new CustomerDomainEvent(UUID.randomUUID(), "Customer",
                        customer.getCustomerId(), customer.getEmail(), customer.getAggregateVersion(), "BasketRecalculatedEvent",
                        Instant.now(), null, null, objectMapper.writeValueAsString(payload));
                customerDomainEventRepository.save(recalculationEvent);
                domainEventPublisher.publish(eventTopic, new DomainEventMessage(recalculationEvent.getEventId(),
                        recalculationEvent.getAggregateType(), recalculationEvent.getAggregateId(),
                        recalculationEvent.getAggregateVersion(), recalculationEvent.getEventType(),
                        recalculationEvent.getOccurredAt(), recalculationEvent.getCorrelationId(),
                        recalculationEvent.getCausationId(), payload));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Unable to serialize basket recalculation event payload", exception);
            }
        });
    }

    private void initializeAggregateVersion(Customer customer) {
        if (!customer.hasAggregateVersion()) {
            customer.initializeAggregateVersion(customerDomainEventRepository.findMaxAggregateVersion("Customer",
                    customer.getCustomerId()));
        }
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String asString(Object value) {
        return value instanceof String string ? string : null;
    }

    private Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}