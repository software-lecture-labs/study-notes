package com.lectures.business.design.domain;

public record PostalAddress(String street, String city, String postalCode, String country) {

    /**
     * Compact constructor: runs before the fields are assigned.
     */
    public PostalAddress {
        street = requireText(street, "street");
        city = requireText(city, "city");
        postalCode = requireText(postalCode, "postalCode");
        country = requireText(country, "country");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }
}
