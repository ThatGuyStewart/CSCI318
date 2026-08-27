package com.retail.customer.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retail.customer.domain.Address;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.ContactMethod;
import com.retail.customer.domain.Customer;
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
import com.retail.customer.repository.CustomerRepository;

@Service
@Transactional
public class CustomerService {

    private static final String CUSTOMER_NOT_FOUND_ID = "Customer not found with id: ";
    private static final String CUSTOMER_NOT_FOUND_EMAIL = "Customer not found with email: ";
    private static final String CUSTOMER_NOT_FOUND_PHONE = "Customer not found with phone: ";

    private final CustomerRepository customerRepository;
    private final BasketRepository basketRepository;

    public CustomerService(CustomerRepository customerRepository, BasketRepository basketRepository) {
        this.customerRepository = customerRepository;
        this.basketRepository = basketRepository;
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
        Customer savedCustomer = customerRepository.save(customer);

        // Automatically initialize basket for the customer
        Basket basket = new Basket(savedCustomer.getId());
        basketRepository.save(basket);

        return toCustomerResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + id));
        return toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_EMAIL + email));
        return toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByPhone(String phone) {
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_PHONE + phone));
        return toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByState(String state) {
        return customerRepository.findByAddress_StateIgnoreCase(state).stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByCountry(String country) {
        return customerRepository.findByAddress_CountryIgnoreCase(country).stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByPostcode(Integer postcode) {
        return customerRepository.findByAddress_Postcode(postcode).stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getCustomerEvents(LocalDate date, LocalDate from, LocalDate to) {
        return buildCustomerEvents(date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getCustomerEventsById(Long customerId, LocalDate date, LocalDate from, LocalDate to) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + customerId));
        List<DomainEventEnvelope> events = buildCustomerEvents(date, from, to);
        return events.stream()
                .filter(event -> event.getAggregateId().equals(customerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getCustomerEventsByEmail(String email, LocalDate date, LocalDate from, LocalDate to) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_EMAIL + email));
        List<DomainEventEnvelope> events = buildCustomerEvents(date, from, to);
        return events.stream()
                .filter(event -> event.getAggregateId().equals(customer.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getCustomerEventsByPhone(String phone, LocalDate date, LocalDate from, LocalDate to) {
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_PHONE + phone));
        List<DomainEventEnvelope> events = buildCustomerEvents(date, from, to);
        return events.stream()
                .filter(event -> event.getAggregateId().equals(customer.getId()))
                .toList();
    }

    private List<DomainEventEnvelope> buildCustomerEvents(LocalDate date, LocalDate from, LocalDate to) {
        List<DomainEventEnvelope> events = new ArrayList<>();
        for (Customer customer : customerRepository.findAll()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("customerId", customer.getId());
            payload.put("name", customer.getName());
            payload.put("email", customer.getEmail());
            payload.put("phone", customer.getPhone());
            payload.put("contactMethod", customer.getContactMethod());
            events.add(new DomainEventEnvelope("CustomerCreatedEvent", customer.getId(), payload));
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

    public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + id));

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

        Customer updated = customerRepository.save(customer);
        return toCustomerResponse(updated);
    }

    public AddressResponse updateCustomerAddress(Long id, AddressUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + id));

        Address address = toAddress(request);
        customer.setAddress(address);
        customerRepository.save(customer);

        return toAddressResponse(id, address);
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND_ID + id));

        basketRepository.deleteById(id);
        customerRepository.delete(customer);
    }

    public CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getContactMethod(),
                toAddressDto(customer.getAddress())
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
