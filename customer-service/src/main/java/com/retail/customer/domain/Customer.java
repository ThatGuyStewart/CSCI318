package com.retail.customer.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactMethod contactMethod = ContactMethod.Email;

    @Embedded
    private Address address;

    private Long aggregateVersion;

    public Customer() {
    }

    public Customer(String name, String email, String phone, ContactMethod contactMethod, Address address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.contactMethod = contactMethod != null ? contactMethod : ContactMethod.Email;
        this.address = address;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ContactMethod getContactMethod() {
        return contactMethod;
    }

    public void setContactMethod(ContactMethod contactMethod) {
        this.contactMethod = contactMethod;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public long nextAggregateVersion() {
        aggregateVersion = getAggregateVersion() + 1;
        return aggregateVersion;
    }

    public long getAggregateVersion() {
        return aggregateVersion != null ? aggregateVersion : 0;
    }

    public boolean hasAggregateVersion() {
        return aggregateVersion != null;
    }

    public void initializeAggregateVersion(long aggregateVersion) {
        if (this.aggregateVersion == null) {
            this.aggregateVersion = aggregateVersion;
        }
    }
}
