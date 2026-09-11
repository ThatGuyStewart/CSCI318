package com.retail.customer.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.customer.domain.Address;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.ContactMethod;
import com.retail.customer.domain.Customer;
import com.retail.customer.domain.CustomerDomainEvent;
import com.retail.customer.domain.CustomerView;
import com.retail.customer.dto.AddressDto;
import com.retail.customer.dto.AddressResponse;
import com.retail.customer.dto.AddressUpdateRequest;
import com.retail.customer.dto.CustomerCreateRequest;
import com.retail.customer.dto.CustomerResponse;
import com.retail.customer.dto.CustomerUpdateRequest;
import com.retail.customer.dto.DomainEventEnvelope;
import com.retail.customer.exception.BadRequestException;
import com.retail.customer.exception.ConflictException;
import com.retail.customer.exception.ResourceNotFoundException;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerDomainEventRepository;
import com.retail.customer.repository.CustomerRepository;
import com.retail.customer.repository.CustomerViewRepository;

@Service
@Transactional
@SuppressWarnings("null")
public class CustomerService {

    private static final String CUSTOMER_NOT_FOUND_ID = "Customer not found with id: ";
    private static final String CUSTOMER_NOT_FOUND_EMAIL = "Customer not found with email: ";
    private static final String CUSTOMER_NOT_FOUND_PHONE = "Customer not found with phone: ";
    private static final String CUSTOMER_ID_REQUIRED = "Customer ID is required";
    private static final String CUSTOMER_REQUIRED = "Customer is required";
    private static final String CUSTOMER_AGGREGATE_TYPE = "Customer";

    private final CustomerRepository customerRepository;
    private final CustomerViewRepository customerViewRepository;
    private final BasketRepository basketRepository;
    private final CustomerDomainEventRepository customerDomainEventRepository;
    private final ObjectMapper objectMapper;
        private final DomainEventPublisher domainEventPublisher;
        private final String eventTopic;

        public CustomerService(CustomerRepository customerRepository, CustomerViewRepository customerViewRepository,
            BasketRepository basketRepository,
            CustomerDomainEventRepository customerDomainEventRepository, ObjectMapper objectMapper,
            DomainEventPublisher domainEventPublisher, @Value("${retail.events.topic}") String eventTopic) {
        this.customerRepository = customerRepository;
        this.customerViewRepository = customerViewRepository;
        this.basketRepository = basketRepository;
        this.customerDomainEventRepository = customerDomainEventRepository;
        this.objectMapper = objectMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopic = eventTopic;
    }

    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new BadRequestException("Phone is required");
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Customer with email " + request.getEmail() + " already exists");
        }
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Customer with phone " + request.getPhone() + " already exists");
        }

        Address address = toAddress(request.getAddress());
        ContactMethod contactMethod = request.getContactMethod() != null ? request.getContactMethod() : ContactMethod.Email;
        Customer customer = new Customer(request.getName(), request.getEmail(), request.getPhone(), contactMethod, address);
        customer.nextAggregateVersion();
        Customer savedCustomer = customerRepository.save(customer);

        // Automatically initialize basket for the customer
        Basket basket = new Basket(savedCustomer.getCustomerId());
        basketRepository.save(basket);
        appendCustomerEvent("CustomerCreatedEvent", savedCustomer);
        upsertCustomerView(savedCustomer, basket.getTotal());

        return toCustomerResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerViewRepository.findAll().stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Long customerId = requireCustomerId(id);
        CustomerView customer = customerViewRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + customerId));
        return toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        CustomerView customer = customerViewRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_EMAIL + email));
        return toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByPhone(String phone) {
        CustomerView customer = customerViewRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_PHONE + phone));
        return toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByState(String state) {
        return customerViewRepository.findByAddress_StateIgnoreCase(state).stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByCountry(String country) {
        return customerViewRepository.findByAddress_CountryIgnoreCase(country).stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByPostcode(Integer postcode) {
        return customerViewRepository.findByAddress_Postcode(postcode).stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getCustomerEvents(LocalDate date, LocalDate from, LocalDate to) {
        return findCustomerEvents(date, from, to, null);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getCustomerEventsById(Long customerId, LocalDate date, LocalDate from, LocalDate to) {
        Long resolvedCustomerId = requireCustomerId(customerId);
        boolean customerExists = customerViewRepository.existsById(resolvedCustomerId)
                || customerDomainEventRepository.existsByAggregateTypeAndAggregateId(
                        CUSTOMER_AGGREGATE_TYPE, resolvedCustomerId);
        if (!customerExists) {
            throw new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + resolvedCustomerId);
        }
        return findCustomerEvents(date, from, to, resolvedCustomerId);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getCustomerEventsByEmail(String email, LocalDate date, LocalDate from, LocalDate to) {
        CustomerDomainEvent customerEvent = customerDomainEventRepository
            .findFirstByAggregateTypeAndCustomerEmailOrderByOccurredAtDescAggregateVersionDesc(
                CUSTOMER_AGGREGATE_TYPE, email)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_EMAIL + email));
        return findCustomerEvents(date, from, to, customerEvent.getAggregateId());
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getCustomerEventsByPhone(String phone, LocalDate date, LocalDate from, LocalDate to) {
        CustomerView customer = customerViewRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_PHONE + phone));
        return findCustomerEvents(date, from, to, customer.getCustomerId());
    }

        private List<DomainEventEnvelope> findCustomerEvents(LocalDate date, LocalDate from, LocalDate to,
            Long customerId) {
        Instant start = null;
        Instant end = null;
        if (date != null) {
            start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else if (from != null || to != null) {
            start = (from != null ? from : LocalDate.of(1970, 1, 1)).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = (to != null ? to.plusDays(1) : LocalDate.now(ZoneOffset.UTC).plusDays(1))
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        List<CustomerDomainEvent> events;
        if (start == null) {
            events = customerId == null
                ? customerDomainEventRepository.findByAggregateTypeOrderByOccurredAtAscAggregateVersionAsc(
                    CUSTOMER_AGGREGATE_TYPE)
                : customerDomainEventRepository.findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc(
                    CUSTOMER_AGGREGATE_TYPE, customerId);
        } else {
            events = customerId == null
                ? customerDomainEventRepository.findByAggregateTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
                    CUSTOMER_AGGREGATE_TYPE, start, end)
                : customerDomainEventRepository.findByAggregateTypeAndAggregateIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByAggregateVersionAsc(
                    CUSTOMER_AGGREGATE_TYPE, customerId, start, end);
        }
        return events.stream()
                .map(this::toDomainEventEnvelope)
                .toList();
    }

    public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {
        Long customerId = requireCustomerId(id);
        Customer customer = customerRepository.findByIdForUpdate(customerId)
            .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + customerId));
        initializeAggregateVersion(customer);

        if (request.getName() != null && !request.getName().isBlank()) {
            customer.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(customer.getEmail()) && customerRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Customer with email " + request.getEmail() + " already exists");
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (!request.getPhone().equals(customer.getPhone()) && customerRepository.existsByPhone(request.getPhone())) {
                throw new ConflictException("Customer with phone " + request.getPhone() + " already exists");
            }
            customer.setPhone(request.getPhone());
        }
        if (request.getContactMethod() != null) {
            customer.setContactMethod(request.getContactMethod());
        }
        if (request.getAddress() != null) {
            customer.setAddress(toAddress(request.getAddress()));
        }

        customer.nextAggregateVersion();
        Customer updated = customerRepository.save(Objects.requireNonNull(customer, CUSTOMER_REQUIRED));
        appendCustomerEvent("CustomerUpdatedEvent", updated);
        upsertCustomerView(updated, currentBasketTotal(customerId));
        return toCustomerResponse(updated);
    }

    public AddressResponse updateCustomerAddress(Long id, AddressUpdateRequest request) {
        Long customerId = requireCustomerId(id);
        Customer customer = customerRepository.findByIdForUpdate(customerId)
            .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + customerId));
        initializeAggregateVersion(customer);

        Address address = toAddress(request);
        customer.setAddress(address);
        customer.nextAggregateVersion();
        Customer updated = customerRepository.save(Objects.requireNonNull(customer, CUSTOMER_REQUIRED));
        appendCustomerEvent("CustomerUpdatedEvent", updated);
        upsertCustomerView(updated, currentBasketTotal(customerId));

        return toAddressResponse(customerId, address);
    }

    public void deleteCustomer(Long id) {
        Long customerId = requireCustomerId(id);
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + customerId));

        initializeAggregateVersion(customer);
        customer.nextAggregateVersion();
        appendCustomerEvent("CustomerDeletedEvent", customer);
        basketRepository.deleteById(customerId);
        customerRepository.delete(Objects.requireNonNull(customer, CUSTOMER_REQUIRED));
        customerViewRepository.deleteById(customerId);
    }

    private void appendCustomerEvent(String eventType, Customer customer) {
        Map<String, Object> payload = customerPayload(customer);
        String serializedPayload;
        try {
            serializedPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize customer event payload", exception);
        }
        CustomerDomainEvent event = new CustomerDomainEvent(UUID.randomUUID(), CUSTOMER_AGGREGATE_TYPE,
            customer.getCustomerId(), customer.getEmail(), customer.getAggregateVersion(), eventType, Instant.now(), null, null,
            serializedPayload);
        customerDomainEventRepository.save(event);
        domainEventPublisher.publish(eventTopic, new DomainEventMessage(event.getEventId(), event.getAggregateType(),
            event.getAggregateId(), event.getAggregateVersion(), event.getEventType(), event.getOccurredAt(),
            event.getCorrelationId(), event.getCausationId(), payload));
    }

    private void initializeAggregateVersion(Customer customer) {
        if (!customer.hasAggregateVersion()) {
            customer.initializeAggregateVersion(customerDomainEventRepository.findMaxAggregateVersion(
                    CUSTOMER_AGGREGATE_TYPE, customer.getCustomerId()));
        }
    }

    private Map<String, Object> customerPayload(Customer customer) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", customer.getCustomerId());
        payload.put("name", customer.getName());
        payload.put("email", customer.getEmail());
        payload.put("phone", customer.getPhone());
        payload.put("contactMethod", customer.getContactMethod());
        payload.put("address", toAddressDto(customer.getAddress()));
        return payload;
    }

    private void upsertCustomerView(Customer customer, Double basketTotal) {
        customerViewRepository.save(new CustomerView(customer.getCustomerId(), customer.getName(), customer.getEmail(),
                customer.getPhone(), customer.getContactMethod(), customer.getAddress(), basketTotal));
    }

    private Double currentBasketTotal(Long customerId) {
        return basketRepository.findById(customerId).map(Basket::getTotal).orElse(0.0);
    }

    private DomainEventEnvelope toDomainEventEnvelope(CustomerDomainEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() { });
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
            throw new IllegalStateException("Unable to deserialize customer event payload", exception);
        }
    }

    private Long requireCustomerId(Long customerId) {
        return Objects.requireNonNull(customerId, CUSTOMER_ID_REQUIRED);
    }

    public CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getContactMethod(),
                toAddressDto(customer.getAddress())
        );
    }

    public CustomerResponse toCustomerResponse(CustomerView customer) {
        return new CustomerResponse(customer.getCustomerId(), customer.getName(), customer.getEmail(), customer.getPhone(),
                customer.getContactMethod(), toAddressDto(customer.getAddress()));
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

    public AddressResponse toAddressResponse(Long customerId, Address address) {
        if (address == null) return new AddressResponse(customerId, null, null, null, null, null, null, null, null);
        return new AddressResponse(
                customerId,
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
