package com.retail.customer.consumer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.Customer;
import com.retail.customer.domain.ProcessedProductEvent;
import com.retail.customer.repository.CustomerDomainEventRepository;
import com.retail.customer.repository.CustomerRepository;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.ProcessedProductEventRepository;
import com.retail.customer.service.CustomerEventRecorder;

@Component
@ConditionalOnProperty(name = "retail.events.consumer.enabled", havingValue = "true")
@SuppressWarnings("null")
public class ProductEventConsumer {

    private final BasketRepository basketRepository;
    private final ProcessedProductEventRepository processedProductEventRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDomainEventRepository customerDomainEventRepository;
    private final CustomerEventRecorder customerEventRecorder;

    public ProductEventConsumer(BasketRepository basketRepository,
            ProcessedProductEventRepository processedProductEventRepository,
            CustomerRepository customerRepository, CustomerDomainEventRepository customerDomainEventRepository,
            CustomerEventRecorder customerEventRecorder) {
        this.basketRepository = basketRepository;
        this.processedProductEventRepository = processedProductEventRepository;
        this.customerRepository = customerRepository;
        this.customerDomainEventRepository = customerDomainEventRepository;
        this.customerEventRecorder = customerEventRecorder;
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
            appendBasketRecalculatedEvent(saved, event.eventId());
        }
    }

    private void appendBasketRecalculatedEvent(Basket basket, java.util.UUID causationId) {
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
            customerEventRecorder.record(customer, "BasketRecalculatedEvent", payload, causationId);
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