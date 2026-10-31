package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderNotFoundException;
import com.lectures.business.design.shared.kernel.Specification;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId orderId);

    List<Order> findAll();

    List<Order> findAll(Specification<Order> specification);

    long count();

    default Order getById(OrderId orderId) {
        return findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
