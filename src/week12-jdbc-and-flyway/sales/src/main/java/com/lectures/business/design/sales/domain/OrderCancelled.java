package com.lectures.business.design.sales.domain;

public record OrderCancelled(OrderId orderId, CustomerId customerId, OrderStatus previousStatus)
        implements OrderEvent {

}
