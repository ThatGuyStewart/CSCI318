package com.retail.customer.dto;

import com.retail.customer.domain.ContactMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CustomerCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    private ContactMethod contactMethod = ContactMethod.Email;

    @NotNull
    @Valid
    private AddressDto address;

    public CustomerCreateRequest() {
    }

    public CustomerCreateRequest(String name, String email, String phone, ContactMethod contactMethod, AddressDto address) {
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
        this.contactMethod = contactMethod != null ? contactMethod : ContactMethod.Email;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }
}
