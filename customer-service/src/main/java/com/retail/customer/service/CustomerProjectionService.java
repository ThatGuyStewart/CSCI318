package com.retail.customer.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.Address;
import com.retail.customer.domain.ContactMethod;
import com.retail.customer.domain.CustomerProjectionCheckpoint;
import com.retail.customer.domain.CustomerView;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerProjectionCheckpointRepository;
import com.retail.customer.repository.CustomerViewRepository;

@Service
public class CustomerProjectionService {

    private static final String CUSTOMER_AGGREGATE_TYPE = "Customer";

    private final CustomerViewRepository customerViewRepository;
    private final BasketRepository basketRepository;
    private final CustomerProjectionCheckpointRepository checkpointRepository;
    private final ObjectMapper objectMapper;

    public CustomerProjectionService(CustomerViewRepository customerViewRepository, BasketRepository basketRepository,
            CustomerProjectionCheckpointRepository checkpointRepository, ObjectMapper objectMapper) {
        this.customerViewRepository = customerViewRepository;
        this.basketRepository = basketRepository;
        this.checkpointRepository = checkpointRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean apply(DomainEventMessage event) {
        Objects.requireNonNull(event, "Customer event is required");
        if (!CUSTOMER_AGGREGATE_TYPE.equals(event.aggregateType())) {
            throw new IllegalArgumentException("Unexpected aggregate type for Customer projection");
        }
        Long customerId = Objects.requireNonNull(event.aggregateId(), "Customer event aggregate ID is required");
        if (event.aggregateVersion() < 1) {
            throw new IllegalArgumentException("Customer event must have a positive aggregate version");
        }

        CustomerProjectionCheckpoint checkpoint = checkpointRepository.findById(customerId).orElse(null);
        if (checkpoint != null && event.aggregateVersion() <= checkpoint.getAggregateVersion()) {
            return false;
        }

        switch (event.eventType()) {
            case "CustomerCreatedEvent" -> customerViewRepository.save(
                    Objects.requireNonNull(toCustomerView(event, 0.0)));
            case "CustomerUpdatedEvent" -> {
                CustomerView existing = customerViewRepository.findById(customerId).orElse(null);
                double basketTotal = existing != null ? existing.getBasketTotal()
                        : basketRepository.findById(customerId).map(basket -> basket.getTotal()).orElse(0.0);
                customerViewRepository.save(
                        Objects.requireNonNull(toCustomerView(event, basketTotal)));
            }
            case "CustomerDeletedEvent" -> customerViewRepository.deleteById(customerId);
            case "BasketItemAddedEvent", "BasketItemRemovedEvent", "BasketRecalculatedEvent" -> {
                CustomerView existing = requireCustomerView(customerId);
                JsonNode total = objectMapper.valueToTree(event.payload()).path("total");
                if (!total.isNumber()) {
                    throw new IllegalArgumentException("Customer basket event is missing its total");
                }
                existing.setBasketTotal(total.doubleValue());
                customerViewRepository.save(existing);
            }
            case "BasketClearedEvent", "BasketClearedAfterOrderEvent" -> {
                CustomerView existing = requireCustomerView(customerId);
                existing.setBasketTotal(0.0);
                customerViewRepository.save(existing);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported Customer event type: " + event.eventType());
        }

        checkpointRepository.save(new CustomerProjectionCheckpoint(customerId, event.aggregateVersion()));
        return true;
    }

    private CustomerView requireCustomerView(Long customerId) {
        Long resolvedCustomerId = Objects.requireNonNull(customerId);
        return customerViewRepository.findById(resolvedCustomerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Customer projection does not exist: " + resolvedCustomerId));
    }

    private CustomerView toCustomerView(DomainEventMessage event, Double basketTotal) {
        JsonNode payload = objectMapper.valueToTree(event.payload());
        if (payload == null || !payload.isObject()
                || !payload.path("customerId").isIntegralNumber()
                || payload.path("customerId").longValue() != event.aggregateId()
                || !payload.path("name").isTextual()
                || !payload.path("email").isTextual()
                || !payload.path("phone").isTextual()
                || !payload.path("contactMethod").isTextual()) {
            throw new IllegalArgumentException("Customer event payload is missing required projection fields");
        }

        ContactMethod contactMethod;
        try {
            contactMethod = ContactMethod.valueOf(payload.path("contactMethod").asText());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Customer event contains an unknown contact method", exception);
        }

        Address address = null;
        JsonNode addressNode = payload.get("address");
        if (addressNode != null && !addressNode.isNull()) {
            try {
                address = objectMapper.treeToValue(addressNode, Address.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Customer event contains an invalid address", exception);
            }
        }
        return new CustomerView(event.aggregateId(), payload.path("name").asText(), payload.path("email").asText(),
                payload.path("phone").asText(), contactMethod, address, basketTotal);
    }
}
