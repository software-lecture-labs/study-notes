package com.lectures.business.design.domain;

public class OrderNotModifiableException extends DomainException {
    private final int orderId;
    private final OrderStatus status;
    private final String attemptedAction;
    
    public OrderNotModifiableException(int orderId, OrderStatus status, String attemptedAction) {
        super("cannot " + attemptedAction + " on order " + orderId + " in status " + status);
        this.orderId = orderId;
        this.status = status;
        this.attemptedAction = attemptedAction;
    }

    public int orderId() {
        return orderId;
    }

    public OrderStatus status() {
        return status;
    }

    public String attemptedAction() {
        return attemptedAction;
    }
}
