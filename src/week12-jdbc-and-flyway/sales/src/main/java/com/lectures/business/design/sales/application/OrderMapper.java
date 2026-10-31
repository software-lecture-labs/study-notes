package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.api.CustomerRevenue;
import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.shared.kernel.Address;
import com.lectures.business.design.shared.kernel.Money;
import java.util.List;
import java.util.Objects;

public final class OrderMapper {

    public OrderSummary toSummary(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        return new OrderSummary(
                order.orderId().value(),
                order.customerId().value(),
                order.status().name(),
                order.orderDate(),
                order.total().amount(),
                order.total().currency().getCurrencyCode(),
                order.lines().size(),
                order.shippingAddress().map(Address::city).orElse(null));
    }

    public List<OrderSummary> toSummaries(List<Order> orders) {
        Objects.requireNonNull(orders, "orders must not be null");
        return orders.stream().map(this::toSummary).toList();
    }

    public CustomerRevenue toRevenue(CustomerId customerId, Money revenue) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(revenue, "revenue must not be null");

        return new CustomerRevenue(
                customerId.value(),
                revenue.amount(),
                revenue.currency().getCurrencyCode());
    }
}
