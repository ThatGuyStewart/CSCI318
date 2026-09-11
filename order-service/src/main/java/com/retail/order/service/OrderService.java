package com.retail.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.order.client.CustomerClient;
import com.retail.order.client.ProductClient;
import com.retail.order.domain.Address;
import com.retail.order.domain.Order;
import com.retail.order.domain.OrderDomainEvent;
import com.retail.order.domain.OrderItem;
import com.retail.order.domain.OrderStatus;
import com.retail.order.domain.OrderSummaryView;
import com.retail.order.dto.AddressDto;
import com.retail.order.dto.BasketDto;
import com.retail.order.dto.BasketItemDto;
import com.retail.order.dto.CustomerDto;
import com.retail.order.dto.DomainEventEnvelope;
import com.retail.order.dto.OrderCreateRequest;
import com.retail.order.dto.OrderItemDto;
import com.retail.order.dto.OrderItemEventPayload;
import com.retail.order.dto.OrderResponse;
import com.retail.order.dto.OrderStatusUpdateRequest;
import com.retail.order.dto.ProductDto;
import com.retail.order.exception.BadRequestException;
import com.retail.order.exception.ResourceNotFoundException;
import com.retail.order.repository.OrderDomainEventRepository;
import com.retail.order.repository.OrderRepository;
import com.retail.order.repository.OrderSummaryViewRepository;

@Service
@Transactional
@SuppressWarnings("SpellCheckingInspection")
public class OrderService {

    private static final String ORDER_NOT_FOUND_ID = "Order not found with id: ";
    private static final String ORDER_ID_REQUIRED = "Order ID is required";
    private static final String ORDER_REQUIRED = "Order is required";
    private static final String ORDER_AGGREGATE_TYPE = "Order";

    private final OrderRepository orderRepository;
    private final OrderSummaryViewRepository orderSummaryViewRepository;
    private final OrderDomainEventRepository orderDomainEventRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final String eventTopic;

    public OrderService(OrderRepository orderRepository, OrderSummaryViewRepository orderSummaryViewRepository,
            OrderDomainEventRepository orderDomainEventRepository,
            CustomerClient customerClient,
            ProductClient productClient, ObjectMapper objectMapper,
            DomainEventPublisher domainEventPublisher, @Value("${retail.events.topic}") String eventTopic) {
        this.orderRepository = orderRepository;
        this.orderSummaryViewRepository = orderSummaryViewRepository;
        this.orderDomainEventRepository = orderDomainEventRepository;
        this.customerClient = customerClient;
        this.productClient = productClient;
        this.objectMapper = objectMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopic = eventTopic;
    }

    public OrderResponse createOrder(OrderCreateRequest request) {
        Long customerId = requireCustomerId(request);
        CustomerDto customer = findCustomer(customerId);
        Address address = resolveAddress(request, customer);
        boolean usesCustomerBasket = request.getItems() == null;
        List<OrderItem> orderItems = resolveOrderItems(request, customerId);
        double total = calculateTotal(orderItems);

        Order order = new Order(customerId, address, orderItems, total, OrderStatus.Placed);
        long orderPlacedVersion = order.nextAggregateVersion();
        long basketUsedVersion = usesCustomerBasket ? order.nextAggregateVersion() : orderPlacedVersion;
        Order saved = orderRepository.save(Objects.requireNonNull(order, ORDER_REQUIRED));

        appendOrderEvent("OrderPlacedEvent", saved, orderPlacedVersion);
        if (usesCustomerBasket) {
            appendOrderEvent("BasketUsedForOrderEvent", saved, basketUsedVersion);
        }

        upsertOrderSummary(saved, LocalDateTime.now(ZoneOffset.UTC));
        return toOrderResponse(saved);
    }

    private Long requireCustomerId(OrderCreateRequest request) {
        Long customerId = request.getCustomerId();
        if (customerId == null) {
            throw new BadRequestException("Customer ID is required");
        }
        return customerId;
    }

    private CustomerDto findCustomer(Long customerId) {
        return customerClient.getCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private Address resolveAddress(OrderCreateRequest request, CustomerDto customer) {
        if (request.getAddress() != null) {
            return toAddress(request.getAddress());
        }
        if (customer.getAddress() != null) {
            return toAddress(customer.getAddress());
        }
        throw new BadRequestException("An order delivery address is required when the customer has no address");
    }

    private List<OrderItem> resolveOrderItems(OrderCreateRequest request, Long customerId) {
        if (request.getItems() != null) {
            if (request.getItems().isEmpty()) {
                throw new BadRequestException("Order items must not be empty");
            }
            return resolveOrderItemsFromRequest(request.getItems());
        }

        Optional<BasketDto> basketOpt = customerClient.getCustomerBasket(customerId);
        if (basketOpt.isEmpty() || basketOpt.get().getItems() == null || basketOpt.get().getItems().isEmpty()) {
            throw new BadRequestException("No items provided and customer basket is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (BasketItemDto item : basketOpt.get().getItems()) {
            orderItems.add(new OrderItem(item.getProductId(), item.getName(), item.getPrice(), item.getQuantity(),
                    item.getSubtotal()));
        }
        return orderItems;
    }

    private List<OrderItem> resolveOrderItemsFromRequest(List<OrderItemDto> itemDtos) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDto itemDto : itemDtos) {
            orderItems.add(buildOrderItem(itemDto));
        }
        return orderItems;
    }

    private OrderItem buildOrderItem(OrderItemDto itemDto) {
        validateOrderItem(itemDto);

        int qty = resolveQuantity(itemDto.getQuantity());
        ProductSnapshot snapshot = resolveProductSnapshot(itemDto);
        return new OrderItem(itemDto.getProductId(), snapshot.name(), snapshot.price(), qty);
    }

    private void validateOrderItem(OrderItemDto itemDto) {
        if (itemDto.getProductId() == null) {
            throw new BadRequestException("Product ID is required for each order item");
        }
    }

    private int resolveQuantity(Integer requestedQty) {
        if (requestedQty == null || requestedQty <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        return requestedQty;
    }

    private ProductSnapshot resolveProductSnapshot(OrderItemDto itemDto) {
        ProductDto product = productClient.getProductById(itemDto.getProductId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId()));
        Double productPrice = product.getPrice();
        return new ProductSnapshot(product.getName(), productPrice != null ? productPrice : 0.0d);
    }

    private record ProductSnapshot(String name, double price) {
    }

    private double calculateTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToDouble(orderItem -> {
                    Double subtotal = orderItem.getSubtotal();
                    return subtotal != null ? subtotal : 0.0d;
                })
                .sum();
    }

    public OrderResponse cancelOrder(Long id) {
        Long orderId = Objects.requireNonNull(id, ORDER_ID_REQUIRED);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_ID + orderId));
        initializeAggregateVersion(order);

        if (order.getStatus() == OrderStatus.Placed || order.getStatus() == OrderStatus.Pending) {
            order.setStatus(OrderStatus.Cancelled);
            order.nextAggregateVersion();
            Order saved = orderRepository.save(order);
            appendOrderEvent("OrderCancelledEvent", saved);
            upsertOrderSummary(saved, existingCreatedAt(orderId));
            return toOrderResponse(saved);
        } else {
            order.nextAggregateVersion();
            Order saved = orderRepository.save(order);
            appendOrderEvent("OrderCancelFailedEvent", saved);
            return toOrderResponse(saved);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Long orderId = Objects.requireNonNull(id, ORDER_ID_REQUIRED);
        OrderSummaryView order = orderSummaryViewRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_ID + orderId));
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        return orderSummaryViewRepository.findByCustomerIdWithItems(customerId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerEmail(String email) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByEmail(email);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        Long customerId = Objects.requireNonNull(customerOpt.get().getCustomerId(), "Customer ID is required");
        return orderSummaryViewRepository.findByCustomerIdWithItems(customerId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerPhone(String phone) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByPhone(phone);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        Long customerId = Objects.requireNonNull(customerOpt.get().getCustomerId(), "Customer ID is required");
        return orderSummaryViewRepository.findByCustomerIdWithItems(customerId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByProductId(Long productId) {
        return orderSummaryViewRepository.findByProductIdWithItems(productId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsByCustomerId(Long customerId, LocalDate date, LocalDate from,
            LocalDate to) {
        Set<Long> orderIds = orderSummaryViewRepository.findByCustomerId(customerId).stream()
                .map(order -> Objects.requireNonNull(order, ORDER_REQUIRED).getOrderId())
                .collect(java.util.stream.Collectors.toSet());
        return getOrderEventsForAggregateIds(orderIds, date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsById(Long orderId, LocalDate date, LocalDate from, LocalDate to) {
        Long resolvedOrderId = Objects.requireNonNull(orderId, ORDER_ID_REQUIRED);
        if (!orderDomainEventRepository.existsByAggregateTypeAndAggregateId(ORDER_AGGREGATE_TYPE, resolvedOrderId)) {
            throw new ResourceNotFoundException(ORDER_NOT_FOUND_ID + resolvedOrderId);
        }
        return findOrderEvents(date, from, to, resolvedOrderId, null);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsByCustomerEmail(String email, LocalDate date, LocalDate from,
            LocalDate to) {
        Optional<CustomerDto> customer = customerClient.getCustomerByEmail(email);
        if (customer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with email: " + email);
        }
        Long customerId = customer.get().getCustomerId();
        Set<Long> orderIds = orderSummaryViewRepository.findByCustomerId(customerId).stream()
                .map(order -> Objects.requireNonNull(order, ORDER_REQUIRED).getOrderId())
                .collect(java.util.stream.Collectors.toSet());
        return getOrderEventsForAggregateIds(orderIds, date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsByCustomerPhone(String phone, LocalDate date, LocalDate from,
            LocalDate to) {
        Optional<CustomerDto> customer = customerClient.getCustomerByPhone(phone);
        if (customer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with phone: " + phone);
        }
        Long customerId = customer.get().getCustomerId();
        Set<Long> orderIds = orderSummaryViewRepository.findByCustomerId(customerId).stream()
                .map(order -> Objects.requireNonNull(order, ORDER_REQUIRED).getOrderId())
                .collect(java.util.stream.Collectors.toSet());
        return getOrderEventsForAggregateIds(orderIds, date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsByProductId(Long productId, LocalDate date, LocalDate from,
            LocalDate to) {
        Set<Long> orderIds = orderSummaryViewRepository.findByProductId(productId).stream()
                .map(order -> Objects.requireNonNull(order, ORDER_REQUIRED).getOrderId())
                .collect(java.util.stream.Collectors.toSet());
        return getOrderEventsForAggregateIds(orderIds, date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEvents(LocalDate date, LocalDate from, LocalDate to) {
        return findOrderEvents(date, from, to, null, null);
    }

    private List<DomainEventEnvelope> getOrderEventsForAggregateIds(Set<Long> orderIds, LocalDate date,
            LocalDate from, LocalDate to) {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return findOrderEvents(date, from, to, null, orderIds);
    }

    private List<DomainEventEnvelope> findOrderEvents(LocalDate date, LocalDate from, LocalDate to, Long orderId,
            Set<Long> orderIds) {
        Instant start = null;
        Instant end = null;
        if (date != null) {
            start = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            end = date.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        } else if (from != null || to != null) {
            start = (from != null ? from : LocalDate.of(1970, 1, 1)).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            end = (to != null ? to.plusDays(1) : LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1))
                    .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        }
        List<OrderDomainEvent> events;
        if (start == null) {
            events = orderId != null
                    ? orderDomainEventRepository.findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc(
                            ORDER_AGGREGATE_TYPE, orderId)
                    : orderIds != null
                            ? orderDomainEventRepository
                                    .findByAggregateTypeAndAggregateIdInOrderByOccurredAtAscAggregateVersionAsc(
                                            ORDER_AGGREGATE_TYPE, orderIds)
                            : orderDomainEventRepository.findByAggregateTypeOrderByOccurredAtAscAggregateVersionAsc(
                                    ORDER_AGGREGATE_TYPE);
        } else {
            events = orderId != null
                    ? orderDomainEventRepository
                            .findByAggregateTypeAndAggregateIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByAggregateVersionAsc(
                                    ORDER_AGGREGATE_TYPE, orderId, start, end)
                    : orderIds != null
                            ? orderDomainEventRepository
                                    .findByAggregateTypeAndAggregateIdInAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
                                            ORDER_AGGREGATE_TYPE, orderIds, start, end)
                            : orderDomainEventRepository
                                    .findByAggregateTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
                                            ORDER_AGGREGATE_TYPE, start, end);
        }
        return events.stream()
                .map(this::toDomainEventEnvelope)
                .toList();
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        Long orderId = Objects.requireNonNull(id, ORDER_ID_REQUIRED);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_ID + orderId));
        initializeAggregateVersion(order);

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        order.nextAggregateVersion();
        Order saved = orderRepository.save(Objects.requireNonNull(order, ORDER_REQUIRED));
        appendOrderEvent("OrderStatusChangedEvent", saved);
        upsertOrderSummary(saved, existingCreatedAt(orderId));
        return toOrderResponse(saved);
    }

    private void appendOrderEvent(String eventType, Order order) {
        appendOrderEvent(eventType, order, order.getAggregateVersion());
    }

    private void appendOrderEvent(String eventType, Order order, long aggregateVersion) {
        try {
            Map<String, Object> payload = orderPayload(order);
            OrderDomainEvent event = new OrderDomainEvent(UUID.randomUUID(), ORDER_AGGREGATE_TYPE,
                    order.getOrderId(), aggregateVersion, eventType, Instant.now(), null, null,
                    objectMapper.writeValueAsString(payload));
            orderDomainEventRepository.save(event);
            domainEventPublisher.publish(eventTopic,
                    new DomainEventMessage(event.getEventId(), event.getAggregateType(),
                            event.getAggregateId(), event.getAggregateVersion(), event.getEventType(),
                            event.getOccurredAt(),
                            event.getCorrelationId(), event.getCausationId(), payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize order event payload", exception);
        }
    }

    private void initializeAggregateVersion(Order order) {
        if (!order.hasAggregateVersion()) {
            order.initializeAggregateVersion(orderDomainEventRepository.findMaxAggregateVersion(
                    ORDER_AGGREGATE_TYPE, order.getOrderId()));
        }
    }

    private Map<String, Object> orderPayload(Order order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getOrderId());
        payload.put("customerId", order.getCustomerId());
        payload.put("address", toAddressDto(order.getAddress()));
        payload.put("items", order.getItems().stream()
                .map(item -> new OrderItemEventPayload(item.getProductId(), item.getName(), item.getPrice(),
                        item.getQuantity(), item.getSubtotal()))
                .toList());
        payload.put("total", order.getTotal());
        payload.put("status", order.getStatus());
        return payload;
    }

    private void upsertOrderSummary(Order order, LocalDateTime createdAt) {
        orderSummaryViewRepository.save(new OrderSummaryView(order.getOrderId(), order.getCustomerId(), order.getTotal(),
                order.getStatus(), createdAt, order.getAddress(), order.getItems()));
    }

    private LocalDateTime existingCreatedAt(Long orderId) {
        Long resolvedOrderId = Objects.requireNonNull(orderId, ORDER_ID_REQUIRED);
        return orderSummaryViewRepository.findById(resolvedOrderId)
                .map(view -> Objects.requireNonNull(view.getCreatedAt()))
                .orElseGet(() -> LocalDateTime.now(ZoneOffset.UTC));
    }

    private DomainEventEnvelope toDomainEventEnvelope(OrderDomainEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() {
            });
            DomainEventEnvelope envelope = new DomainEventEnvelope();
            envelope.setEventId(event.getEventId());
            envelope.setAggregateType(event.getAggregateType());
            envelope.setAggregateId(event.getAggregateId());
            envelope.setAggregateVersion(event.getAggregateVersion());
            envelope.setEventType(event.getEventType());
            envelope.setOccurredAt(event.getOccurredAt());
            envelope.setCorrelationId(event.getCorrelationId());
            envelope.setCausationId(event.getCausationId());
            envelope.setPayload(payload);
            return envelope;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize order event payload", exception);
        }
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(i -> new OrderItemDto(i.getProductId(), i.getName(), i.getPrice(), i.getQuantity(),
                        i.getSubtotal()))
                .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                toAddressDto(order.getAddress()),
                itemDtos,
                order.getTotal(),
                order.getStatus());
    }

    public OrderResponse toOrderResponse(OrderSummaryView order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(i -> new OrderItemDto(i.getProductId(), i.getName(), i.getPrice(), i.getQuantity(),
                        i.getSubtotal()))
                .toList();
        return new OrderResponse(order.getOrderId(), order.getCustomerId(), toAddressDto(order.getAddress()), itemDtos,
                order.getTotal(), order.getStatus());
    }

    public Address toAddress(AddressDto dto) {
        if (dto == null)
            return null;
        return new Address(
                dto.getUnitNumber(),
                dto.getStreetNumber(),
                dto.getStreet(),
                dto.getSuburb(),
                dto.getCity(),
                dto.getPostcode(),
                dto.getState(),
                dto.getCountry());
    }

    public AddressDto toAddressDto(Address address) {
        if (address == null)
            return null;
        return new AddressDto(
                address.getUnitNumber(),
                address.getStreetNumber(),
                address.getStreet(),
                address.getSuburb(),
                address.getCity(),
                address.getPostcode(),
                address.getState(),
                address.getCountry());
    }
}
