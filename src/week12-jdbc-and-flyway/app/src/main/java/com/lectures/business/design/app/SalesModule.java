package com.lectures.business.design.app;

import com.lectures.business.design.sales.application.CustomerNotifier;
import com.lectures.business.design.sales.application.InProcessEventPublisher;
import com.lectures.business.design.sales.application.OrderActivityLog;
import com.lectures.business.design.sales.application.OrderMapper;
import com.lectures.business.design.sales.application.OrderService;
import com.lectures.business.design.sales.application.SalesReport;
import com.lectures.business.design.sales.application.ShipmentNotificationHandler;
import com.lectures.business.design.sales.domain.OrderEvent;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.OrderShipped;
import com.lectures.business.design.sales.domain.SequentialOrderIdGenerator;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryCustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryOrderRepository;
import com.lectures.business.design.sales.persistence.OrderRepository;
import java.time.Clock;
import java.util.Objects;

public final class SalesModule {

    private final CustomerRepository customers;
    private final OrderService orderService;
    private final SalesReport salesReport;
    private final OrderActivityLog activityLog = new OrderActivityLog();

    private SalesModule(OrderRepository orders, CustomerRepository customers, Clock clock,
            int firstOrderId, CustomerNotifier notifier) {
        this.customers = customers;

        InProcessEventPublisher publisher = new InProcessEventPublisher()
                .subscribe(OrderEvent.class, activityLog)
                .subscribe(OrderShipped.class, new ShipmentNotificationHandler(notifier));

        OrderMapper mapper = new OrderMapper();
        OrderFactory factory = new OrderFactory(new SequentialOrderIdGenerator(firstOrderId), clock);

        this.orderService = new OrderService(orders, customers, factory, mapper, clock, publisher);
        this.salesReport = new SalesReport(orders, mapper);
    }

    public static SalesModule inMemory(Clock clock, int firstOrderId, CustomerNotifier notifier) {
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(notifier, "notifier must not be null");
        return new SalesModule(new InMemoryOrderRepository(), new InMemoryCustomerRepository(),
                clock, firstOrderId, notifier);
    }

    public CustomerRepository customers() {
        return customers;
    }

    public OrderService orderService() {
        return orderService;
    }

    public SalesReport salesReport() {
        return salesReport;
    }

    public OrderActivityLog activityLog() {
        return activityLog;
    }
}
