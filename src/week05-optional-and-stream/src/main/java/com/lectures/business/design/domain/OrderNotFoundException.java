package com.lectures.business.design.domain;

public class OrderNotFoundException extends DomainException {

    private final int orderId;

    public OrderNotFoundException(int orderId) {
        super("no order with id " + orderId);
        this.orderId = orderId;
    }

    public int orderId() {
        return orderId;
    }
}
