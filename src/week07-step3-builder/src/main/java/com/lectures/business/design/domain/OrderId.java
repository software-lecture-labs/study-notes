package com.lectures.business.design.domain;

public record OrderId(int value) implements Comparable<OrderId> {

    public OrderId {
        if (value <= 0) {
            throw new IllegalArgumentException("orderId must be positive: " + value);
        }
    }

    public static OrderId of(int value) {
        return new OrderId(value);
    }

    @Override
    public int compareTo(OrderId other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
