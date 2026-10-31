package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.Money;
import com.lectures.business.design.shared.kernel.Specification;
import java.time.LocalDate;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> inStatus(OrderStatus status) {
        return order -> order.status() == status;
    }

    public static Specification<Order> cancelled() {
        return inStatus(OrderStatus.CANCELLED);
    }

    public static Specification<Order> notCancelled() {
        return cancelled().not();
    }

    public static Specification<Order> placedBy(CustomerId customerId) {
        return order -> order.customerId().equals(customerId);
    }

    public static Specification<Order> worthAtLeast(Money threshold) {
        return order -> order.total().compareTo(threshold) >= 0;
    }

    public static Specification<Order> orderedOnOrAfter(LocalDate date) {
        return order -> !order.orderDate().isBefore(date);
    }

    public static Specification<Order> shippedTo(String country) {
        return order -> order.shippingAddress()
                .filter(address -> address.isIn(country))
                .isPresent();
    }
}
