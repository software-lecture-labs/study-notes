package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.api.OrderLineRequest;
import com.lectures.business.design.sales.api.PlaceOrderCommand;
import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.InvalidStateTransitionException;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderShipped;
import com.lectures.business.design.sales.domain.OrderStatus;
import com.lectures.business.design.sales.domain.SequentialOrderIdGenerator;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryCustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryOrderRepository;
import com.lectures.business.design.sales.persistence.OrderRepository;
import com.lectures.business.design.shared.kernel.Address;
import com.lectures.business.design.shared.kernel.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceEventsTest {

    private final List<DomainEvent> published = new ArrayList<>();
    private final List<String> notifications = new ArrayList<>();
    private CustomerNotifier mailGateway = (customerId, message) -> notifications.add(customerId + ": " + message);
    private OrderRepository orders;
    private InProcessEventPublisher publisher;
    private OrderService service;

    @BeforeEach
    void setUp() {
        SteppingClock clock = new SteppingClock("1996-07-04T09:00:00Z");
        orders = new InMemoryOrderRepository();
        CustomerRepository customers = new InMemoryCustomerRepository();
        customers.save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));

        publisher = new InProcessEventPublisher()
                .subscribe(DomainEvent.class, published::add)
                .subscribe(OrderShipped.class, new ShipmentNotificationHandler(
                        (customerId, message) -> mailGateway.notify(customerId, message)));

        service = new OrderService(orders, customers,
                new OrderFactory(new SequentialOrderIdGenerator(10248), clock),
                new OrderMapper(), clock, publisher);
    }

    private int placeAndConfirm() {
        int id = service.placeOrder(new PlaceOrderCommand("VINET",
                List.of(OrderLineRequest.of(11, "14.00", 10)))).orderId();
        service.confirm(id);
        return id;
    }

    @Test
    @DisplayName("each use case publishes what the aggregate recorded, once")
    void useCasesPublishRecordedEvents() {
        int id = placeAndConfirm();
        service.ship(id);

        assertThat(published).extracting(e -> e.getClass().getSimpleName())
                .containsExactly("OrderConfirmed", "OrderShipped");
        assertThat(notifications)
                .containsExactly("VINET: Order 10248 shipped on 1996-07-04 to Reims");
    }

    @Test
    @DisplayName("observers run after save: what they read is already stored")
    void observersSeeTheSavedState() {
        List<OrderStatus> seenByObserver = new ArrayList<>();
        publisher.subscribe(OrderShipped.class,
                e -> seenByObserver.add(orders.getById(e.orderId()).status()));

        service.ship(placeAndConfirm());

        assertThat(seenByObserver).containsExactly(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("a rejected command publishes nothing")
    void rejectedCommandPublishesNothing() {
        int id = service.placeOrder(new PlaceOrderCommand("VINET",
                List.of(OrderLineRequest.of(11, "14.00", 10)))).orderId();

        assertThatThrownBy(() -> service.ship(id))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(published).isEmpty();
    }

    @Test
    @DisplayName("the dual-write gap: the order is stored, the notification is lost")
    void aFailingObserverLeavesHalfTheWorkDone() {
        int id = placeAndConfirm();
        mailGateway = (customerId, message) -> {
            throw new IllegalStateException("mail server down");
        };

        assertThatThrownBy(() -> service.ship(id)).hasMessage("mail server down");
        assertThat(orders.getById(OrderId.of(id)).status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(orders.getById(OrderId.of(id)).pullEvents()).isEmpty();
        assertThat(notifications).isEmpty();
        assertThatThrownBy(() -> service.ship(id))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
