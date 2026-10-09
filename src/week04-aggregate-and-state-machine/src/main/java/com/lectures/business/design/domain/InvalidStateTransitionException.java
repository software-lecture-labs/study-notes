package com.lectures.business.design.domain;

public class InvalidStateTransitionException extends DomainException {
    private final int orderId;
    private final OrderStatus from;
    private final OrderStatus to;
    public InvalidStateTransitionException(int orderId, OrderStatus from, OrderStatus to) {
        super("order " + orderId + " cannot move from " + from + " to " + to);
        this.orderId = orderId;
        this.from = from;
        this.to = to;
    }

    public int orderId() {
        return orderId;
    }

    public OrderStatus from() {
       return from;
    }

    public OrderStatus to() {
        return to;
    }
}
