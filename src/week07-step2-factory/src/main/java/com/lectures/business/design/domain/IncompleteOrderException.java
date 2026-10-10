package com.lectures.business.design.domain;

public class IncompleteOrderException extends DomainException {

    public enum MissingPart {
        LINES,
        SHIPPING_ADDRESS
    }
    private final OrderId orderId;
    private final MissingPart missingPart;

    public IncompleteOrderException(OrderId orderId, MissingPart missingPart) {
        super("order " + orderId + " cannot be confirmed: " + missingPart + " missing");
        this.orderId = orderId;
        this.missingPart = missingPart;
    }

    public OrderId orderId() {
        return orderId;
    }

    public MissingPart missingPart() {
        return missingPart;
    }
}
