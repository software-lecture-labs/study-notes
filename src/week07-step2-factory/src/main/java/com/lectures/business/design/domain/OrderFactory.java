package com.lectures.business.design.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class OrderFactory {

    private final OrderIdGenerator idGenerator;
    private final Clock clock;

    public OrderFactory(OrderIdGenerator idGenerator, Clock clock) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Order draftFor(Customer customer) {
        Objects.requireNonNull(customer, "customer must not be null");
        return draftFor(customer, customer.address());
    }

    public Order draftFor(Customer customer, Address destination) {
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Order order = new Order(idGenerator.next(), customer.customerId(), LocalDate.now(clock));
        order.shipTo(destination);
        return order;
    }
}
