package com.retail.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retail.order.client.CustomerClient;
import com.retail.order.client.NotificationClient;
import com.retail.order.client.ProductClient;
import com.retail.order.domain.Address;
import com.retail.order.domain.Order;
import com.retail.order.domain.OrderItem;
import com.retail.order.domain.OrderStatus;
import com.retail.order.dto.AddressDto;
import com.retail.order.dto.BasketDto;
import com.retail.order.dto.BasketItemDto;
import com.retail.order.dto.CustomerDto;
import com.retail.order.dto.DomainEventEnvelope;
import com.retail.order.dto.OrderCreateRequest;
import com.retail.order.dto.OrderItemDto;
import com.retail.order.dto.OrderResponse;
import com.retail.order.dto.OrderStatusUpdateRequest;
import com.retail.order.dto.ProductDto;
import com.retail.order.exception.BadRequestException;
import com.retail.order.exception.ResourceNotFoundException;
import com.retail.order.repository.OrderRepository;

@Service
@Transactional
@SuppressWarnings("SpellCheckingInspection")
public class OrderService {

    private static final String ORDER_NOT_FOUND_ID = "Order not found with id: ";
    private static final String ORDER_ID_REQUIRED = "Order ID is required";

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final NotificationClient notificationClient;

    public OrderService(OrderRepository orderRepository,
                        CustomerClient customerClient,
                        ProductClient productClient,
                        NotificationClient notificationClient) {
        this.orderRepository = orderRepository;
        this.customerClient = customerClient;
        this.productClient = productClient;
        this.notificationClient = notificationClient;
    }

    public OrderResponse createOrder(OrderCreateRequest request) {
        Long customerId = requireCustomerId(request);
        CustomerDto customer = findCustomer(customerId);
        Address address = resolveAddress(request, customer);
        List<OrderItem> orderItems = resolveOrderItems(request, customerId);
        double total = calculateTotal(orderItems);

        Order order = new Order(customerId, address, orderItems, total, OrderStatus.Placed);
        Order saved = orderRepository.save(Objects.requireNonNull(order, "Order is required"));

        notificationClient.sendNotificationToCustomer(saved.getCustomerId(), "Your order #" + saved.getId() + " has been placed.");
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
        return new Address(null, 1, "Default St", "Default", "City", 1000, "State", "Country");
    }

    private List<OrderItem> resolveOrderItems(OrderCreateRequest request, Long customerId) {
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            return resolveOrderItemsFromRequest(request.getItems());
        }

        Optional<BasketDto> basketOpt = customerClient.getCustomerBasket(customerId);
        if (basketOpt.isEmpty() || basketOpt.get().getItems() == null || basketOpt.get().getItems().isEmpty()) {
            throw new BadRequestException("No items provided and customer basket is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (BasketItemDto item : basketOpt.get().getItems()) {
            orderItems.add(new OrderItem(item.getProductId(), item.getName(), item.getPrice(), item.getQuantity(), item.getSubtotal()));
        }
        customerClient.clearCustomerBasket(customerId);
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
        return requestedQty != null && requestedQty > 0 ? requestedQty : 1;
    }

    private ProductSnapshot resolveProductSnapshot(OrderItemDto itemDto) {
        ProductDto product = productClient.getProductById(itemDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId()));
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_ID + orderId));

        if (order.getStatus() == OrderStatus.Placed || order.getStatus() == OrderStatus.Pending) {
            order.setStatus(OrderStatus.Cancelled);
            Order saved = orderRepository.save(order);
            notificationClient.sendNotificationToCustomer(saved.getCustomerId(), "Your order #" + saved.getId() + " has been cancelled.");
            return toOrderResponse(saved);
        } else {
            // Unchanged status, notify customer
            notificationClient.sendNotificationToCustomer(order.getCustomerId(),
                    "Order #" + order.getId() + " could not be cancelled because it is in status: " + order.getStatus() + ". Please contact customer service.");
            return toOrderResponse(order);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Long orderId = Objects.requireNonNull(id, ORDER_ID_REQUIRED);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_ID + orderId));
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerEmail(String email) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByEmail(email);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        Long customerId = Objects.requireNonNull(customerOpt.get().getId(), "Customer ID is required");
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerPhone(String phone) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByPhone(phone);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        Long customerId = Objects.requireNonNull(customerOpt.get().getId(), "Customer ID is required");
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByProductId(Long productId) {
        return orderRepository.findByProductId(productId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsByCustomerId(Long customerId, LocalDate date, LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return buildOrderEventsForOrders(orders, date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsById(Long orderId, LocalDate date, LocalDate from, LocalDate to) {
        Long resolvedOrderId = Objects.requireNonNull(orderId, ORDER_ID_REQUIRED);
        Order order = orderRepository.findById(resolvedOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_ID + resolvedOrderId));
        return buildOrderEventsForOrders(List.of(order), date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsByCustomerEmail(String email, LocalDate date, LocalDate from, LocalDate to) {
        Optional<CustomerDto> customer = customerClient.getCustomerByEmail(email);
        if (customer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with email: " + email);
        }
        Long customerId = customer.get().getId();
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return buildOrderEventsForOrders(orders, date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsByCustomerPhone(String phone, LocalDate date, LocalDate from, LocalDate to) {
        Optional<CustomerDto> customer = customerClient.getCustomerByPhone(phone);
        if (customer.isEmpty()) {
            throw new ResourceNotFoundException("Customer not found with phone: " + phone);
        }
        Long customerId = customer.get().getId();
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return buildOrderEventsForOrders(orders, date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getOrderEventsByProductId(Long productId, LocalDate date, LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findByProductId(productId);
        return buildOrderEventsForOrders(orders, date, from, to);
    }

    private List<DomainEventEnvelope> buildOrderEventsForOrders(List<Order> orders, LocalDate date, LocalDate from, LocalDate to) {
        List<DomainEventEnvelope> events = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", order.getId());
            payload.put("customerId", order.getCustomerId());
            payload.put("status", order.getStatus());
            payload.put("total", order.getTotal());
            events.add(new DomainEventEnvelope("OrderPlacedEvent", order.getId(), payload));
        }

        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return events.stream().filter(event -> !event.getTimestamp().isBefore(start) && event.getTimestamp().isBefore(end)).toList();
        }
        if (from != null || to != null) {
            LocalDateTime start = (from != null ? from : LocalDate.of(1970, 1, 1)).atStartOfDay();
            LocalDateTime end = (to != null ? to : LocalDate.now(ZoneId.systemDefault())).plusDays(1).atStartOfDay();
            return events.stream().filter(event -> !event.getTimestamp().isBefore(start) && event.getTimestamp().isBefore(end)).toList();
        }
        return events;
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        Long orderId = Objects.requireNonNull(id, ORDER_ID_REQUIRED);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_ID + orderId));

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        Order saved = orderRepository.save(Objects.requireNonNull(order, "Order is required"));
        return toOrderResponse(saved);
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(i -> new OrderItemDto(i.getProductId(), i.getName(), i.getPrice(), i.getQuantity(), i.getSubtotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                toAddressDto(order.getAddress()),
                itemDtos,
                order.getTotal(),
                order.getStatus()
        );
    }

    public Address toAddress(AddressDto dto) {
        if (dto == null) return null;
        return new Address(
                dto.getUnitNumber(),
                dto.getStreetNumber(),
                dto.getStreet(),
                dto.getSuburb(),
                dto.getCity(),
                dto.getPostcode(),
                dto.getState(),
                dto.getCountry()
        );
    }

    public AddressDto toAddressDto(Address address) {
        if (address == null) return null;
        return new AddressDto(
                address.getUnitNumber(),
                address.getStreetNumber(),
                address.getStreet(),
                address.getSuburb(),
                address.getCity(),
                address.getPostcode(),
                address.getState(),
                address.getCountry()
        );
    }
}
