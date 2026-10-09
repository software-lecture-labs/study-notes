package com.lectures.business.design.domain;

public class IncompleteOrderException extends DomainException {

    public enum MissingPart {
        LINES,
        SHIPPING_ADDRESS
    }
    private final int orderId;
    private final MissingPart missingPart;

    public IncompleteOrderException(int orderId, MissingPart missingPart) {
        super("order " + orderId + " cannot be confirmed: " + missingPart + " missing");
        this.orderId = orderId;
        this.missingPart = missingPart;
    }

    public int orderId() {
        return orderId;
    }

    public MissingPart missingPart() {
        return missingPart;
    }
}
