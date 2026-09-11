package com.retail.customer.consumer;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.ProcessedOrderEvent;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerViewRepository;
import com.retail.customer.repository.ProcessedOrderEventRepository;

@Component
@ConditionalOnProperty(name = "retail.events.consumer.enabled", havingValue = "true")
@SuppressWarnings("null")
public class OrderEventConsumer {

    private final BasketRepository basketRepository;
    private final CustomerViewRepository customerViewRepository;
    private final ProcessedOrderEventRepository processedOrderEventRepository;

    public OrderEventConsumer(BasketRepository basketRepository, CustomerViewRepository customerViewRepository,
            ProcessedOrderEventRepository processedOrderEventRepository) {
        this.basketRepository = basketRepository;
        this.customerViewRepository = customerViewRepository;
        this.processedOrderEventRepository = processedOrderEventRepository;
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
        basketRepository.findById(customerId).ifPresent(this::clearBasketAndProject);
    }

    private Long asLong(Map<String, Object> payload) {
        Object customerId = payload.get("customerId");
        return customerId instanceof Number number ? number.longValue() : null;
    }

    private void clearBasketAndProject(Basket basket) {
        basket.clear();
        Basket saved = basketRepository.save(basket);
        customerViewRepository.findById(saved.getCustomerId()).ifPresent(view -> {
            view.setBasketTotal(saved.getTotal());
            customerViewRepository.save(view);
        });
    }
}