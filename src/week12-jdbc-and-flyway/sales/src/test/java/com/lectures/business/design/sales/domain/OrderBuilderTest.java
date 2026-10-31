package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderBuilderTest {
    @Test
    @DisplayName("the defaults produce a usable draft")
    void defaultsAreSane() {
        Order order = OrderBuilder.anOrder().build();
        assertThat(order.orderId()).isEqualTo(OrderId.of(10248));
        assertThat(order.customerId()).isEqualTo(CustomerId.of("VINET"));
        assertThat(order.orderDate()).isEqualTo(LocalDate.of(1996, 7, 4));
        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.shippingAddress()).isPresent();
    }

    @Test
    @DisplayName("lines and discounts are applied through the aggregate")
    void linesAreAdded() {
        Order order = OrderBuilder.anOrder()
                .withLine(11, "14.00", 12)
                .withDiscountedLine(42, "9.80", 10, "0.15")
                .build();
        assertThat(order.total()).isEqualTo(Money.tl("251.30"));
        assertThat(order.hasDiscountedLine()).isTrue();
    }

    @Test
    @DisplayName("the target state is reached through confirm and ship")
    void stateIsAdvanced() {
        Order shipped = OrderBuilder.anOrder().withLine(11, "14.00", 1).shipped().build();
        assertThat(shipped.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(shipped.shippedDate()).contains(LocalDate.of(1996, 7, 16));
        Order cancelled = OrderBuilder.anOrder().cancelled().build();
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("the builder cannot produce an order the domain would reject")
    void domainRulesStillApply() {
        assertThatThrownBy(() -> OrderBuilder.anOrder().confirmed().build())
                .isInstanceOf(IncompleteOrderException.class);
    }

    @Test
    @DisplayName("two builders produce independent orders")
    void buildersDoNotShareState() {
        Order first = OrderBuilder.anOrder().withId(10248).withLine(11, "14.00", 1).build();
        Order second = OrderBuilder.anOrder().withId(10249).withLine(42, "9.80", 2).build();
        assertThat(first.productIds()).containsExactly(11);
        assertThat(second.productIds()).containsExactly(42);
    }
}

