package com.lectures.business.design.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record CustomerId(String value) implements Comparable<CustomerId> {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{5}$");

    public CustomerId {
        String raw = value;
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }

        value = value.strip().toUpperCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "customerId must be exactly five letters (A-Z): " + raw);
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    @Override
    public int compareTo(CustomerId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
