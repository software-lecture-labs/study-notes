package com.lectures.business.design.domain;

public class InvalidStateTransitionException extends DomainException {

    private final OrderId orderId;
    private final OrderStatus from;
    private final OrderStatus to;

    public InvalidStateTransitionException(OrderId orderId, OrderStatus from, OrderStatus to) {
        super("order " + orderId + " cannot move from " + from + " to " + to);
        this.orderId = orderId;
        this.from = from;
        this.to = to;
    }

    public OrderId orderId() {
        return orderId;
    }

    public OrderStatus from() {
        return from;
    }

    public OrderStatus to() {
        return to;
    }
}
