package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.DomainEvent;

public sealed interface OrderEvent extends DomainEvent
        permits OrderConfirmed, OrderShipped, OrderCancelled {

    OrderId orderId();

    CustomerId customerId();
}
