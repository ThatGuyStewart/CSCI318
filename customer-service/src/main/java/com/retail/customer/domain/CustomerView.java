package com.retail.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_views")
public class CustomerView {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactMethod contactMethod;

    @Embedded
    private Address address;

    @Column(nullable = false)
    private Double basketTotal;

    public CustomerView() {
    }

    public CustomerView(Long id, String name, String email, String phone, ContactMethod contactMethod, Address address,
            Double basketTotal) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.contactMethod = contactMethod;
        this.address = address;
        this.basketTotal = basketTotal != null ? basketTotal : 0.0;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public ContactMethod getContactMethod() { return contactMethod; }
    public Address getAddress() { return address; }
    public Double getBasketTotal() { return basketTotal; }
    public void setBasketTotal(Double basketTotal) { this.basketTotal = basketTotal != null ? basketTotal : 0.0; }
}