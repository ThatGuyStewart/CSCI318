package com.retail.customer.controller;

import com.retail.customer.dto.*;
import com.retail.customer.service.BasketService;
import com.retail.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping({"/id/{id}", "/{id}"})
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

    @GetMapping({"/{id}/event", "/id/{id}/event"})
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

    @PutMapping({"/id/{id}", "/{id}"})
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable("id") Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping({"/id/{id}/address", "/{id}/address"})
    public ResponseEntity<AddressResponse> updateCustomerAddress(@PathVariable("id") Long id, @Valid @RequestBody AddressUpdateRequest request) {
        AddressResponse response = customerService.updateCustomerAddress(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/id/{id}", "/{id}"})
    public ResponseEntity<Void> deleteCustomer(@PathVariable("id") Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------
    // Basket Endpoints (Feature C3)
    // -------------------------------------------------------------

    @GetMapping({"/id/{id}/basket", "/{id}/basket"})
    public ResponseEntity<BasketResponse> getBasket(@PathVariable("id") Long id) {
        BasketResponse response = basketService.getBasketByCustomerId(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/id/{id}/basket/items", "/{id}/basket/items"})
    public ResponseEntity<BasketResponse> addItemToBasket(@PathVariable("id") Long id, @Valid @RequestBody BasketAddRequest request) {
        BasketResponse response = basketService.addItemToBasket(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/id/{id}/basket/items", "/{id}/basket/items"})
    public ResponseEntity<BasketResponse> removeItemFromBasket(@PathVariable("id") Long id, @Valid @RequestBody BasketRemoveRequest request) {
        BasketResponse response = basketService.removeItemFromBasket(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/id/{id}/basket/clear", "/{id}/basket/clear"})
    public ResponseEntity<Void> clearBasket(@PathVariable("id") Long id) {
        basketService.clearBasket(id);
        return ResponseEntity.noContent().build();
    }
}
