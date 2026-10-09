package com.lectures.business.design.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Order {

    private static final int MAX_LINES = 50;

    private final OrderId orderId;
    private final CustomerId customerId;
    private final LocalDate orderDate;
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;
    private Address shippingAddress;
    private LocalDate shippedDate;

    public Order(OrderId orderId, CustomerId customerId, LocalDate orderDate) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.orderDate = Objects.requireNonNull(orderDate, "orderDate must not be null");
    }

    public void addLine(int productId, Money unitPrice, int quantity, BigDecimal discount) {
        requireModifiable("add a line");
        int existing = indexOfProduct(productId);
        if (existing >= 0) {
            lines.set(existing, lines.get(existing).withAdditionalQuantity(quantity));
            return;
        }
        if (lines.size() == MAX_LINES) {
            throw new IllegalStateException("an order cannot hold more than " + MAX_LINES + " lines");
        }
        lines.add(new OrderLine(productId, unitPrice, quantity, discount));
    }

    public void removeLine(int productId) {
        requireModifiable("remove a line");
        int index = indexOfProduct(productId);
        if (index < 0) {
            throw new IllegalArgumentException("product is not on this order: " + productId);
        }
        lines.remove(index);
    }

    public void confirm() {
        requireModifiable("confirm");
        if (lines.isEmpty()) {
            throw new IncompleteOrderException(orderId, IncompleteOrderException.MissingPart.LINES);
        }

        if (shippingAddress == null) {
            throw new IncompleteOrderException(
                    orderId, IncompleteOrderException.MissingPart.SHIPPING_ADDRESS);
        }
        transitionTo(OrderStatus.CONFIRMED);

    }

    public void shipTo(Address address) {
        requireModifiable("change the shipping address");
        shippingAddress = Objects.requireNonNull(address, "address must not be null");
    }

    public void ship(LocalDate date) {
        requireTransition(OrderStatus.SHIPPED);
        Objects.requireNonNull(date, "shipped date must not be null");
        if (date.isBefore(orderDate)) {
            throw new IllegalArgumentException("shipped date cannot precede the order date");
        }
        shippedDate = date;
        status = OrderStatus.SHIPPED;
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    // --- behaviour end ---    
    public Money total() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(Money::plus)
                .orElse(Money.tl("0"));
    }

    // --- state begin ---
    public OrderId orderId() {
        return orderId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public LocalDate orderDate() {
        return orderDate;
    }

    public OrderStatus status() {
        return status;
    }

    public Optional<Address> shippingAddress() {
        return Optional.ofNullable(shippingAddress);
    }

    public Optional<LocalDate> shippedDate() {
        return Optional.ofNullable(shippedDate);
    }

    public List<OrderLine> lines() {
        return List.copyOf(lines);
    }
    // --- state end ---

    // --- helpers begin ---
    private int indexOfProduct(int productId) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).productId() == productId) {
                return i;
            }
        }
        return -1;
    }

    private void requireModifiable(String action) {
        if (!status.isModifiable()) {
            throw new OrderNotModifiableException(orderId, status, action);
        }
    }

    private void requireTransition(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(orderId, status, target);
        }
    }

    private void transitionTo(OrderStatus target) {
        requireTransition(target);
        status = target;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Order order)) {
            return false;
        }
        return orderId == order.orderId;
    }

    @Override
    public int hashCode() {
        return orderId.hashCode();
    }
    // --- helpers end ---

    // --- queries start ---
    
    // Same as before

    // --- queries end ---
}
