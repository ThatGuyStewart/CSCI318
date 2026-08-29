package com.retail.customer.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.customer.client.ProductClient;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.BasketItem;
import com.retail.customer.domain.Customer;
import com.retail.customer.domain.CustomerDomainEvent;
import com.retail.customer.dto.BasketAddRequest;
import com.retail.customer.dto.BasketItemResponse;
import com.retail.customer.dto.BasketRemoveRequest;
import com.retail.customer.dto.BasketResponse;
import com.retail.customer.dto.ProductDto;
import com.retail.customer.exception.BadRequestException;
import com.retail.customer.exception.ResourceNotFoundException;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerDomainEventRepository;
import com.retail.customer.repository.CustomerRepository;
import com.retail.customer.repository.CustomerViewRepository;

@Service
@Transactional
@SuppressWarnings("null")
public class BasketService {

    private static final String CUSTOMER_ID_REQUIRED = "Customer ID is required";
    private static final String CUSTOMER_AGGREGATE_TYPE = "Customer";

    private final BasketRepository basketRepository;
    private final CustomerRepository customerRepository;
    private final CustomerViewRepository customerViewRepository;
    private final CustomerDomainEventRepository customerDomainEventRepository;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;
        private final DomainEventPublisher domainEventPublisher;
        private final String eventTopic;

        public BasketService(BasketRepository basketRepository, CustomerRepository customerRepository,
            CustomerViewRepository customerViewRepository,
            CustomerDomainEventRepository customerDomainEventRepository, ProductClient productClient,
            ObjectMapper objectMapper, DomainEventPublisher domainEventPublisher,
            @Value("${retail.events.topic}") String eventTopic) {
        this.basketRepository = basketRepository;
        this.customerRepository = customerRepository;
        this.customerViewRepository = customerViewRepository;
        this.customerDomainEventRepository = customerDomainEventRepository;
        this.productClient = productClient;
        this.objectMapper = objectMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopic = eventTopic;
    }

    @Transactional(readOnly = true)
    public BasketResponse getBasketByCustomerId(Long customerId) {
        Long resolvedCustomerId = requireCustomerId(customerId);
        if (!customerViewRepository.existsById(resolvedCustomerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + resolvedCustomerId);
        }

        Basket basket = basketRepository.findById(resolvedCustomerId)
                .orElseGet(() -> basketRepository.save(new Basket(resolvedCustomerId)));

        return toBasketResponse(basket);
    }

    public BasketResponse addItemToBasket(Long customerId, BasketAddRequest request) {
        Long resolvedCustomerId = requireCustomerId(customerId);
        Customer customer = findCustomerForUpdate(resolvedCustomerId);
        if (request.getProductId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Basket basket = basketRepository.findById(resolvedCustomerId)
            .orElseGet(() -> new Basket(resolvedCustomerId));

        // Fetch product info from Product Service
        Optional<ProductDto> productOpt = productClient.getProductById(request.getProductId());
        String productName = "Product " + request.getProductId();
        Double productPrice = 10.0; // fallback if product service offline/standalone

        if (productOpt.isPresent()) {
            productName = productOpt.get().getName();
            productPrice = productOpt.get().getPrice();
        }

        basket.addItem(request.getProductId(), productName, productPrice, request.getQuantity());
        Basket saved = basketRepository.save(basket);
    customer.nextAggregateVersion();
    customerRepository.save(customer);
        appendBasketEvent("BasketItemAddedEvent", customer, saved, request.getProductId(), request.getQuantity());
        updateCustomerViewTotal(saved);

        return toBasketResponse(saved);
    }

    public BasketResponse removeItemFromBasket(Long customerId, BasketRemoveRequest request) {
        Long resolvedCustomerId = requireCustomerId(customerId);
        Customer customer = findCustomerForUpdate(resolvedCustomerId);
        if (request.getProductId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Basket basket = basketRepository.findById(resolvedCustomerId)
            .orElseThrow(() -> new ResourceNotFoundException("Basket not found for customer: " + resolvedCustomerId));

        basket.removeItem(request.getProductId(), request.getQuantity());
        Basket saved = basketRepository.save(basket);
        customer.nextAggregateVersion();
        customerRepository.save(customer);
        appendBasketEvent("BasketItemRemovedEvent", customer, saved, request.getProductId(), request.getQuantity());
        updateCustomerViewTotal(saved);

        return toBasketResponse(saved);
    }

    public void clearBasket(Long customerId) {
        Long resolvedCustomerId = requireCustomerId(customerId);
        basketRepository.findById(resolvedCustomerId).ifPresent(basket -> {
            basket.clear();
            Basket saved = basketRepository.save(basket);
            updateCustomerViewTotal(saved);
        });
    }

    private Long requireCustomerId(Long customerId) {
        return Objects.requireNonNull(customerId, CUSTOMER_ID_REQUIRED);
    }

    private Customer findCustomerForUpdate(Long customerId) {
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        if (!customer.hasAggregateVersion()) {
            customer.initializeAggregateVersion(customerDomainEventRepository.findMaxAggregateVersion(
                    CUSTOMER_AGGREGATE_TYPE, customerId));
        }
        return customer;
    }

    private void appendBasketEvent(String eventType, Customer customer, Basket basket, Long productId, Integer quantity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", basket.getCustomerId());
        payload.put("productId", productId);
        payload.put("quantity", quantity);
        payload.put("items", basket.getItems().stream().map(this::toBasketItemResponse).toList());
        payload.put("total", basket.getTotal());
        try {
            String serializedPayload = objectMapper.writeValueAsString(payload);
            CustomerDomainEvent event = new CustomerDomainEvent(UUID.randomUUID(), CUSTOMER_AGGREGATE_TYPE,
                    basket.getCustomerId(), null, customer.getAggregateVersion(), eventType, Instant.now(), null, null,
                    serializedPayload);
            customerDomainEventRepository.save(event);
            domainEventPublisher.publish(eventTopic, new DomainEventMessage(event.getEventId(), event.getAggregateType(),
                    event.getAggregateId(), event.getAggregateVersion(), event.getEventType(), event.getOccurredAt(),
                    event.getCorrelationId(), event.getCausationId(), payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize basket event payload", exception);
        }
    }

    private void updateCustomerViewTotal(Basket basket) {
        customerViewRepository.findById(basket.getCustomerId()).ifPresent(view -> {
            view.setBasketTotal(basket.getTotal());
            customerViewRepository.save(view);
        });
    }

    public BasketResponse toBasketResponse(Basket basket) {
        List<BasketItemResponse> itemResponses = basket.getItems()
                .stream()
                .map(this::toBasketItemResponse)
                .toList(); // unmodifiable list by contract

        return new BasketResponse(basket.getCustomerId(), itemResponses, basket.getTotal());
    }

    public BasketItemResponse toBasketItemResponse(BasketItem item) {
        return new BasketItemResponse(
                item.getProductId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
