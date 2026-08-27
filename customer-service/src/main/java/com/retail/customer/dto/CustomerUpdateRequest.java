package com.retail.customer.dto;

import com.retail.customer.domain.ContactMethod;
import jakarta.validation.Valid;

public class CustomerUpdateRequest {
    private String name;
    private String email;
    private String phone;
    private ContactMethod contactMethod;
    @Valid
    private AddressDto address;

    public CustomerUpdateRequest() {
    }

    public CustomerUpdateRequest(String name, String email, String phone, ContactMethod contactMethod, AddressDto address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.contactMethod = contactMethod;
        this.address = address;
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

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }
}
