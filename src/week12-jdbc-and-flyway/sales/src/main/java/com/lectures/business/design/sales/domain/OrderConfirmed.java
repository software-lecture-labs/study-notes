package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.Money;

public record OrderConfirmed(OrderId orderId, CustomerId customerId, Money total) implements OrderEvent {

}
