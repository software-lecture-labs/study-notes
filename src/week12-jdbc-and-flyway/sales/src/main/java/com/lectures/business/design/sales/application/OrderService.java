package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.api.OrderLineRequest;
import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.api.PlaceOrderCommand;
import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.sales.persistence.OrderRepository;
import com.lectures.business.design.shared.kernel.Money;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;
import java.util.function.Consumer;

public final class OrderService {

    private static final Currency TRY = Currency.getInstance("TRY");

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final OrderFactory factory;
    private final OrderMapper mapper;
    private final Clock clock;
    private final DomainEventPublisher events;

    public OrderService(OrderRepository orders, CustomerRepository customers,
            OrderFactory factory, OrderMapper mapper, Clock clock, DomainEventPublisher events) {
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
        this.customers = Objects.requireNonNull(customers, "customers must not be null");
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.events = Objects.requireNonNull(events, "events must not be null");
    }

    public OrderSummary placeOrder(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Customer customer = customers.getById(CustomerId.of(command.customerId()));

        Order order = factory.draftFor(customer);
        for (OrderLineRequest line : command.lines()) {
            order.addLine(line.productId(), new Money(line.unitPrice(), TRY),
                    line.quantity(), line.discount());
        }
        orders.save(order);
        return mapper.toSummary(order);
    }

    public OrderSummary confirm(int orderId) {
        return change(orderId, Order::confirm);
    }

    public OrderSummary ship(int orderId) {
        return change(orderId, order -> order.ship(LocalDate.now(clock)));
    }

    public OrderSummary cancel(int orderId) {
        return change(orderId, Order::cancel);
    }

    public OrderSummary getOrder(int orderId) {
        return mapper.toSummary(orders.getById(OrderId.of(orderId)));
    }

    private OrderSummary change(int orderId, Consumer<Order> action) {
        Order order = orders.getById(OrderId.of(orderId));
        action.accept(order);
        orders.save(order);
        events.publishAll(order.pullEvents());
        return mapper.toSummary(order);
    }
}
