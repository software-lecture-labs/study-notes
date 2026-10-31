package com.lectures.business.design.sales.domain;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lectures.business.design.shared.kernel.Money;

class OrderEventsTest {

    private static Order draft() {
        Order order = OrderBuilder.anOrder().withId(10248).forCustomer("VINET")
                .withLine(11, "14.00", 10).build();
        order.pullEvents(); // start every test from a clean slate
        return order;
    }

    @Test
    @DisplayName("editing a draft is not a business fact worth announcing")
    void draftChangesRecordNothing() {
        Order order = draft();
        order.addLine(42, Money.tl("9.80"), 1, java.math.BigDecimal.ZERO);
        order.removeLine(42);

        assertThat(order.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("confirm, ship and cancel each record one event in the past tense")
    void transitionsRecordEvents() {
        Order order = draft();
        order.confirm();
        order.ship(LocalDate.of(1996, 7, 16));

        assertThat(order.pullEvents()).containsExactly(
                new OrderConfirmed(OrderId.of(10248), CustomerId.of("VINET"), Money.tl("140.00")),
                new OrderShipped(OrderId.of(10248), CustomerId.of("VINET"),
                        LocalDate.of(1996, 7, 16), order.shippingAddress().orElseThrow()));
    }

    @Test
    @DisplayName("a cancellation remembers what it interrupted")
    void cancellationCarriesThePreviousStatus() {
        Order order = draft();
        order.confirm();
        order.pullEvents();
        order.cancel();

        assertThat(order.pullEvents())
                .singleElement()
                .isEqualTo(new OrderCancelled(OrderId.of(10248), CustomerId.of("VINET"), OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("pullEvents hands the events over exactly once")
    void pullEventsDrainsTheQueue() {
        Order order = draft();
        order.confirm();

        assertThat(order.pullEvents()).hasSize(1).isUnmodifiable();
        assertThat(order.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("a rejected transition records nothing")
    void rejectedTransitionRecordsNothing() {
        Order order = draft();

        assertThatThrownBy(() -> order.ship(LocalDate.of(1996, 7, 16)))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(order.pullEvents()).isEmpty();
    }
}
