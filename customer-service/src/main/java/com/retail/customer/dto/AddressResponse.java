package com.retail.customer.dto;

public class AddressResponse {
    private Long customerId;
    private Integer unitNumber;
    private Integer streetNumber;
    private String street;
    private String suburb;
    private String city;
    private Integer postcode;
    private String state;
    private String country;

    public AddressResponse() {
    }

    public AddressResponse(Long customerId, Integer unitNumber, Integer streetNumber, String street, String suburb, String city, Integer postcode, String state, String country) {
        this.customerId = customerId;
        this.unitNumber = unitNumber;
        this.streetNumber = streetNumber;
        this.street = street;
        this.suburb = suburb;
        this.city = city;
        this.postcode = postcode;
        this.state = state;
        this.country = country;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Integer getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(Integer unitNumber) {
        this.unitNumber = unitNumber;
    }

    public Integer getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(Integer streetNumber) {
        this.streetNumber = streetNumber;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getSuburb() {
        return suburb;
    }

    public void setSuburb(String suburb) {
        this.suburb = suburb;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getPostcode() {
        return postcode;
    }

    public void setPostcode(Integer postcode) {
        this.postcode = postcode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
