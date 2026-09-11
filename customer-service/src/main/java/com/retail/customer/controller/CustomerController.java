package com.retail.customer.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.retail.customer.dto.AddressResponse;
import com.retail.customer.dto.AddressUpdateRequest;
import com.retail.customer.dto.BasketAddRequest;
import com.retail.customer.dto.BasketRemoveRequest;
import com.retail.customer.dto.BasketResponse;
import com.retail.customer.dto.CustomerCreateRequest;
import com.retail.customer.dto.CustomerResponse;
import com.retail.customer.dto.CustomerUpdateRequest;
import com.retail.customer.dto.DomainEventEnvelope;
import com.retail.customer.service.BasketService;
import com.retail.customer.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;
    private final BasketService basketService;

    public CustomerController(CustomerService customerService, BasketService basketService) {
        this.customerService = customerService;
        this.basketService = basketService;
    }

    // -------------------------------------------------------------
    // Customer Endpoints (Feature C1 / C2)
    // -------------------------------------------------------------

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> listCustomers() {
        List<CustomerResponse> responses = customerService.getAllCustomers();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable("id") Long id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@PathVariable("email") String email) {
        CustomerResponse response = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<CustomerResponse> getCustomerByPhone(@PathVariable("phone") String phone) {
        CustomerResponse response = customerService.getCustomerByPhone(phone);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/state/{state}")
    public ResponseEntity<List<CustomerResponse>> getCustomersByState(@PathVariable("state") String state) {
        return ResponseEntity.ok(customerService.getCustomersByState(state));
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<CustomerResponse>> getCustomersByCountry(@PathVariable("country") String country) {
        return ResponseEntity.ok(customerService.getCustomersByCountry(country));
    }

    @GetMapping("/postcode/{postcode}")
    public ResponseEntity<List<CustomerResponse>> getCustomersByPostcode(@PathVariable("postcode") Integer postcode) {
        return ResponseEntity.ok(customerService.getCustomersByPostcode(postcode));
    }

    @GetMapping("/event")
    public ResponseEntity<List<DomainEventEnvelope>> getCustomerEvents(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(customerService.getCustomerEvents(date, from, to));
    }

    @GetMapping("/{id}/event")
    public ResponseEntity<List<DomainEventEnvelope>> getCustomerEventsById(@PathVariable("id") Long id,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(customerService.getCustomerEventsById(id, date, from, to));
    }

    @GetMapping("/email/{email}/event")
    public ResponseEntity<List<DomainEventEnvelope>> getCustomerEventsByEmail(@PathVariable("email") String email,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(customerService.getCustomerEventsByEmail(email, date, from, to));
    }

    @GetMapping("/phone/{phone}/event")
    public ResponseEntity<List<DomainEventEnvelope>> getCustomerEventsByPhone(@PathVariable("phone") String phone,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(customerService.getCustomerEventsByPhone(phone, date, from, to));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable("id") Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/address")
    public ResponseEntity<AddressResponse> updateCustomerAddress(@PathVariable("id") Long id, @Valid @RequestBody AddressUpdateRequest request) {
        AddressResponse response = customerService.updateCustomerAddress(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("id") Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------
    // Basket Endpoints (Feature C3)
    // -------------------------------------------------------------

    @GetMapping("/{id}/basket")
    public ResponseEntity<BasketResponse> getBasket(@PathVariable("id") Long id) {
        BasketResponse response = basketService.getBasketByCustomerId(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/basket")
    public ResponseEntity<BasketResponse> addItemToBasket(@PathVariable("id") Long id, @Valid @RequestBody BasketAddRequest request) {
        BasketResponse response = basketService.addItemToBasket(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/basket")
    public ResponseEntity<BasketResponse> removeItemFromBasket(@PathVariable("id") Long id, @Valid @RequestBody BasketRemoveRequest request) {
        BasketResponse response = basketService.removeItemFromBasket(id, request);
        return ResponseEntity.ok(response);
    }

}
