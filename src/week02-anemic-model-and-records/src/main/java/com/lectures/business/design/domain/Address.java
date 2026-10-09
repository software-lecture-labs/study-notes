package com.lectures.business.design.domain;

import java.util.Objects;

public final class Address {

    private final String street;
    private final String city;
    private final String postalCode; // can be Value Object
    private final String country;

    public Address(String street, String city, String postalCode, String country) {
        this.street = requireText(street, "street");
        this.city = requireText(city, "city");
        this.postalCode = requireText(postalCode, "postalCode");
        this.country = requireText(country, "country");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    public String street() {
        return street;
    }

    public String city() {
        return city;
    }

    public String postalCode() {
        return postalCode;
    }

    public String country() {
        return country;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Address address)) {
            return false;
        }
        return street.equals(address.street)
                && city.equals(address.city)
                && postalCode.equals(address.postalCode)
                && country.equals(address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, postalCode, country);
    }

    @Override
    public String toString() {
        return "Address[" + street + ", " + postalCode + " " + city + ", " + country + "]";
    }
}
