package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.DomainException;

public class OrderNotFoundException extends DomainException {

    private final OrderId orderId;

    public OrderNotFoundException(OrderId orderId) {
        super("no order with id " + orderId);
        this.orderId = orderId;
    }

    public OrderId orderId() {
        return orderId;
    }
}
