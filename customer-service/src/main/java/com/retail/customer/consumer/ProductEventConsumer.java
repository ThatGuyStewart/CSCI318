package com.retail.customer.consumer;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.ProcessedProductEvent;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.ProcessedProductEventRepository;
import com.retail.customer.repository.CustomerViewRepository;

@Component
@ConditionalOnProperty(name = "retail.events.consumer.enabled", havingValue = "true")
@SuppressWarnings("null")
public class ProductEventConsumer {

    private final BasketRepository basketRepository;
    private final ProcessedProductEventRepository processedProductEventRepository;
    private final CustomerViewRepository customerViewRepository;

    public ProductEventConsumer(BasketRepository basketRepository,
            ProcessedProductEventRepository processedProductEventRepository, CustomerViewRepository customerViewRepository) {
        this.basketRepository = basketRepository;
        this.processedProductEventRepository = processedProductEventRepository;
        this.customerViewRepository = customerViewRepository;
    }

    @KafkaListener(topics = "${retail.events.product-topic}", groupId = "${retail.events.consumer.group-id}")
    @Transactional
    public void onProductEvent(DomainEventMessage event) {
        if (!"ProductUpdatedEvent".equals(event.eventType())
                || processedProductEventRepository.existsById(event.eventId())) {
            return;
        }

        processedProductEventRepository.save(new ProcessedProductEvent(event.eventId()));
        Map<String, Object> payload = event.payload();
        Long productId = asLong(payload.get("productId"));
        String name = asString(payload.get("name"));
        Double price = asDouble(payload.get("price"));
        if (productId == null || name == null || price == null) {
            return;
        }

        for (Basket basket : basketRepository.findByItemsProductId(productId)) {
            boolean updated = false;
            for (var item : basket.getItems()) {
                if (productId.equals(item.getProductId())) {
                    item.setName(name);
                    item.setPrice(price);
                    updated = true;
                }
            }
            if (updated) {
                basket.recalculateTotal();
                Basket saved = basketRepository.save(basket);
                customerViewRepository.findById(saved.getCustomerId()).ifPresent(view -> {
                    view.setBasketTotal(saved.getTotal());
                    customerViewRepository.save(view);
                });
            }
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