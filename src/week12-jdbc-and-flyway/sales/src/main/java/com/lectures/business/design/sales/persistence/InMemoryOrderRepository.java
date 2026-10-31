package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.shared.kernel.Specification;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> orders = new LinkedHashMap<>();

    @Override
    public void save(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        orders.put(order.orderId(), copyOf(order));
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(orders.get(orderId)).map(InMemoryOrderRepository::copyOf);
    }

    @Override
    public List<Order> findAll() {
        return orders.values().stream().map(InMemoryOrderRepository::copyOf).toList();
    }

    @Override
    public List<Order> findAll(Specification<Order> specification) {
        Objects.requireNonNull(specification, "specification must not be null");
        return orders.values().stream()
                .filter(specification::isSatisfiedBy)
                .sorted(Comparator.comparing(Order::orderId))
                .map(InMemoryOrderRepository::copyOf)
                .toList();
    }

    @Override
    public long count() {
        return orders.size();
    }

    private static Order copyOf(Order order) {
        return Order.reconstitute(order.orderId(), order.customerId(), order.orderDate(),
                order.status(), order.shippingAddress().orElse(null),
                order.shippedDate().orElse(null), order.lines());
    }
}
