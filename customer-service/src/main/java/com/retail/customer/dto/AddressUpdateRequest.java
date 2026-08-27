package com.retail.customer.dto;

public class AddressUpdateRequest extends AddressDto {
    public AddressUpdateRequest() {
    }

    public AddressUpdateRequest(Integer unitNumber, Integer streetNumber, String street, String suburb, String city, Integer postcode, String state, String country) {
        super(unitNumber, streetNumber, street, suburb, city, postcode, state, country);
    }
}
