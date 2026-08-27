package com.retail.customer.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Address {
    private Integer unitNumber;
    private Integer streetNumber;
    private String street;
    private String suburb;
    private String city;
    private Integer postcode;
    private String state;
    private String country;

    public Address() {
    }

    public Address(Integer unitNumber, Integer streetNumber, String street, String suburb, String city, Integer postcode, String state, String country) {
        this.unitNumber = unitNumber;
        this.streetNumber = streetNumber;
        this.street = street;
        this.suburb = suburb;
        this.city = city;
        this.postcode = postcode;
        this.state = state;
        this.country = country;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address address)) return false;
        return Objects.equals(unitNumber, address.unitNumber) &&
                Objects.equals(streetNumber, address.streetNumber) &&
                Objects.equals(street, address.street) &&
                Objects.equals(suburb, address.suburb) &&
                Objects.equals(city, address.city) &&
                Objects.equals(postcode, address.postcode) &&
                Objects.equals(state, address.state) &&
                Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unitNumber, streetNumber, street, suburb, city, postcode, state, country);
    }
}
