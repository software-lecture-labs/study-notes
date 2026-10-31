package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.api.CustomerRevenue;
import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.api.SalesTotals;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderSpecifications;
import com.lectures.business.design.sales.persistence.OrderRepository;
import com.lectures.business.design.shared.kernel.Money;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class SalesReport {

    private final OrderRepository repository;
    private final OrderMapper mapper;

    public SalesReport(OrderRepository repository, OrderMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    public Map<String, Long> countByStatus() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        order -> order.status().name(),
                        TreeMap::new,
                        Collectors.counting()));
    }

    public List<CustomerRevenue> revenueByCustomer() {
        return revenueTotals().entrySet().stream()
                .map(entry -> mapper.toRevenue(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<CustomerRevenue> topCustomers(int limit) {
        return revenueTotals().entrySet().stream()
                .sorted(Map.Entry.<CustomerId, Money>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> mapper.toRevenue(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<OrderSummary> ordersOf(String customerId) {
        return mapper.toSummaries(
                repository.findAll(OrderSpecifications.placedBy(CustomerId.of(customerId))));
    }

    public SalesTotals totals() {
        List<Order> earning = earningOrders();
        Money revenue = earning.stream()
                .map(Order::total)
                .reduce(Money.tl("0"), Money::plus);

        return new SalesTotals(
                earning.size(),
                revenue.amount(),
                revenue.currency().getCurrencyCode());
    }

    private Map<CustomerId, Money> revenueTotals() {
        return earningOrders().stream()
                .collect(Collectors.groupingBy(
                        Order::customerId,
                        TreeMap::new,
                        Collectors.reducing(Money.tl("0"), Order::total, Money::plus)));
    }

    private List<Order> earningOrders() {
        return repository.findAll(OrderSpecifications.notCancelled());
    }
}
