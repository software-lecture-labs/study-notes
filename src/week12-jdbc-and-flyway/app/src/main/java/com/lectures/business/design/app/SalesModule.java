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
import com.lectures.business.design.sales.domain.OrderIdGenerator;
import com.lectures.business.design.sales.domain.OrderShipped;
import com.lectures.business.design.sales.domain.SequentialOrderIdGenerator;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryCustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryOrderRepository;
import com.lectures.business.design.sales.persistence.JdbcCustomerRepository;
import com.lectures.business.design.sales.persistence.JdbcOrderRepository;
import com.lectures.business.design.sales.persistence.OrderRepository;
import com.lectures.business.design.sales.persistence.SequenceOrderIdGenerator;
import java.time.Clock;
import java.util.Objects;
import javax.sql.DataSource;

public final class SalesModule {

    private final CustomerRepository customers;
    private final OrderService orderService;
    private final SalesReport salesReport;
    private final OrderActivityLog activityLog = new OrderActivityLog();

    private SalesModule(OrderRepository orders, CustomerRepository customers, OrderIdGenerator ids,
            Clock clock, CustomerNotifier notifier) {
        this.customers = customers;

        InProcessEventPublisher publisher = new InProcessEventPublisher()
                .subscribe(OrderEvent.class, activityLog)
                .subscribe(OrderShipped.class, new ShipmentNotificationHandler(notifier));

        OrderMapper mapper = new OrderMapper();
        OrderFactory factory = new OrderFactory(ids, clock);

        this.orderService = new OrderService(orders, customers, factory, mapper, clock, publisher);
        this.salesReport = new SalesReport(orders, mapper);
    }

    public static SalesModule inMemory(Clock clock, int firstOrderId, CustomerNotifier notifier) {
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(notifier, "notifier must not be null");
        return new SalesModule(new InMemoryOrderRepository(), new InMemoryCustomerRepository(),
                new SequentialOrderIdGenerator(firstOrderId), clock, notifier);
    }

    public static SalesModule jdbc(DataSource dataSource, Clock clock, CustomerNotifier notifier) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(notifier, "notifier must not be null");
        return new SalesModule(new JdbcOrderRepository(dataSource), new JdbcCustomerRepository(dataSource),
                new SequenceOrderIdGenerator(dataSource), clock, notifier);
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
