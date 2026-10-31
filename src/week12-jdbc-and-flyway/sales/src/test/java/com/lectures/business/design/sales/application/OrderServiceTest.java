package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.api.OrderLineRequest;
import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.api.PlaceOrderCommand;
import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.CustomerNotFoundException;
import com.lectures.business.design.sales.domain.InvalidStateTransitionException;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderNotFoundException;
import com.lectures.business.design.sales.domain.SequentialOrderIdGenerator;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryCustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryOrderRepository;
import com.lectures.business.design.sales.persistence.OrderRepository;
import com.lectures.business.design.shared.kernel.Address;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

    private SteppingClock clock;
    private OrderRepository orders;
    private OrderService service;

    @BeforeEach
    void setUp() {
        clock = new SteppingClock("1996-07-04T09:00:00Z");
        orders = new InMemoryOrderRepository();
        CustomerRepository customers = new InMemoryCustomerRepository();
        customers.save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));

        service = new OrderService(orders, customers,
                new OrderFactory(new SequentialOrderIdGenerator(10248), clock),
                new OrderMapper(), clock, event -> {
                });
    }

    private OrderSummary placeVinetOrder() {
        return service.placeOrder(new PlaceOrderCommand("VINET", List.of(
                OrderLineRequest.of(11, "14.00", 12),
                OrderLineRequest.of(42, "9.80", 10))));
    }

    @Test
    @DisplayName("placeOrder drafts, fills and saves an order for a known customer")
    void placeOrderCreatesADraft() {
        OrderSummary summary = placeVinetOrder();

        assertThat(summary.orderId()).isEqualTo(10248);
        assertThat(summary.status()).isEqualTo("DRAFT");
        assertThat(summary.orderDate()).isEqualTo(LocalDate.of(1996, 7, 4));
        assertThat(summary.total()).isEqualByComparingTo(new BigDecimal("266.00"));
        assertThat(summary.shippingCity()).isEqualTo("Reims");
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("an unknown customer stops the use case before anything is saved")
    void unknownCustomerIsRejected() {
        assertThatThrownBy(() -> service.placeOrder(
                new PlaceOrderCommand("ALFKI", List.of(OrderLineRequest.of(11, "14.00", 1)))))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("ship takes 'today' from the injected clock")
    void shipUsesTheClock() {
        int id = placeVinetOrder().orderId();
        service.confirm(id);

        clock.advance(Duration.ofDays(12));
        service.ship(id);

        assertThat(orders.getById(OrderId.of(id)).shippedDate())
                .contains(LocalDate.of(1996, 7, 16));
    }

    @Test
    @DisplayName("the service coordinates, the aggregate still decides")
    void rulesStayInTheAggregate() {
        int id = placeVinetOrder().orderId();

        assertThatThrownBy(() -> service.ship(id))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(service.cancel(id).status()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("an unknown order id surfaces as OrderNotFoundException")
    void unknownOrderIsRejected() {
        assertThatThrownBy(() -> service.confirm(99999))
                .isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> service.getOrder(99999))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("the service refuses to be built without its collaborators")
    void collaboratorsAreRequired() {
        assertThatThrownBy(() -> new OrderService(null, new InMemoryCustomerRepository(),
                new OrderFactory(new SequentialOrderIdGenerator(1), clock), new OrderMapper(), clock, event -> {
                }))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("orders");
    }
}
