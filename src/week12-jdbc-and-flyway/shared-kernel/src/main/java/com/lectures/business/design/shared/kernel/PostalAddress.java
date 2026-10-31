package com.lectures.business.design.shared.kernel;

public record PostalAddress(String street, String city, String postalCode, String country) {

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
