package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    @DisplayName("domain types become plain values at the boundary")
    void domainTypesAreFlattened() {
        Order order = OrderBuilder.anOrder()
                .withId(10248).forCustomer("VINET")
                .withLine(11, "14.00", 10)
                .shipped()
                .build();

        OrderSummary summary = mapper.toSummary(order);
        assertThat(summary.orderId()).isEqualTo(10248);
        assertThat(summary.customerId()).isEqualTo("VINET");
        assertThat(summary.status()).isEqualTo("SHIPPED");
        assertThat(summary.orderDate()).isEqualTo(LocalDate.of(1996, 7, 4));
        assertThat(summary.total()).isEqualByComparingTo(new BigDecimal("140.00"));
        assertThat(summary.currency()).isEqualTo("TRY");
        assertThat(summary.lineCount()).isEqualTo(1);
        assertThat(summary.shippingCity()).isEqualTo("Reims");
    }

    @Test
    @DisplayName("the summary carries no domain object at all")
    void noDomainTypeLeaks() {
        OrderSummary summary = mapper.toSummary(OrderBuilder.anOrder().build());
        assertThat(summary.getClass().getRecordComponents())
                .allSatisfy(component
                        -> assertThat(component.getType().getPackageName())
                                .doesNotStartWith("com.northwind.warmup.domain"));
    }

    @Test
    @DisplayName("a list maps element by element and comes back immutable")
    void listsAreMapped() {
        List<Order> orders = List.of(
                OrderBuilder.anOrder().withId(10248).withLine(11, "14.00", 1).build(),
                OrderBuilder.anOrder().withId(10249).withLine(42, "9.80", 2).build());
        assertThat(mapper.toSummaries(orders))
                .extracting(OrderSummary::orderId)
                .containsExactly(10248, 10249);
        assertThat(mapper.toSummaries(orders)).isUnmodifiable();
    }

    @Test
    @DisplayName("nulls are rejected at the boundary too")
    void nullsAreRejected() {
        assertThatThrownBy(() -> mapper.toSummary(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toSummaries(null)).isInstanceOf(NullPointerException.class);
    }
}
