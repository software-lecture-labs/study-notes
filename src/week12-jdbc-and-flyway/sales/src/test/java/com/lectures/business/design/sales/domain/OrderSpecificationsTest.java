package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.Money;
import com.lectures.business.design.shared.kernel.Specification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class OrderSpecificationsTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(1996, 7, 4);
    private final Order shipped = OrderBuilder.anOrder()
            .withId(10248).forCustomer("VINET").withLine(11, "14.00", 10).shipped().build();
    private final Order cancelled = OrderBuilder.anOrder()
            .withId(10250).forCustomer("TOMSP").withLine(11, "99.00", 5).cancelled().build();

    @Test
    @DisplayName("notCancelled is the negation of cancelled")
    void notCancelledIsTheNegation() {
        assertThat(OrderSpecifications.cancelled().isSatisfiedBy(cancelled)).isTrue();
        assertThat(OrderSpecifications.notCancelled().isSatisfiedBy(cancelled)).isFalse();
        assertThat(OrderSpecifications.notCancelled().isSatisfiedBy(shipped)).isTrue();
    }

    @Test
    @DisplayName("placedBy matches on identity")
    void placedByMatchesIdentity() {
        assertThat(OrderSpecifications.placedBy(CustomerId.of("VINET")).isSatisfiedBy(shipped)).isTrue();
        assertThat(OrderSpecifications.placedBy(CustomerId.of("TOMSP")).isSatisfiedBy(shipped)).isFalse();
    }

    @Test
    @DisplayName("and requires both sides, or requires either")
    void andOrDifferFromEachOther() {
        Specification<Order> vinet = OrderSpecifications.placedBy(CustomerId.of("VINET"));
        Specification<Order> isCancelled = OrderSpecifications.cancelled();
        assertThat(vinet.and(isCancelled).isSatisfiedBy(shipped)).isFalse();
        assertThat(vinet.or(isCancelled).isSatisfiedBy(shipped)).isTrue();
        assertThat(vinet.or(isCancelled).isSatisfiedBy(cancelled)).isTrue();
        assertThat(vinet.and(isCancelled).isSatisfiedBy(cancelled)).isFalse();
    }

    @Test
    @DisplayName("worthAtLeast compares the order total")
    void worthAtLeastComparesTotals() {
        assertThat(OrderSpecifications.worthAtLeast(Money.tl("100.00")).isSatisfiedBy(shipped)).isTrue();
        assertThat(OrderSpecifications.worthAtLeast(Money.tl("140.00")).isSatisfiedBy(shipped)).isTrue();
        assertThat(OrderSpecifications.worthAtLeast(Money.tl("140.01")).isSatisfiedBy(shipped)).isFalse();
    }

    @Test
    @DisplayName("date and destination rules read like business sentences")
    void dateAndDestination() {
        assertThat(OrderSpecifications.orderedOnOrAfter(ORDER_DATE).isSatisfiedBy(shipped)).isTrue();
        assertThat(OrderSpecifications.orderedOnOrAfter(ORDER_DATE.plusDays(1)).isSatisfiedBy(shipped)).isFalse();
        assertThat(OrderSpecifications.shippedTo("France").isSatisfiedBy(shipped)).isTrue();
        assertThat(OrderSpecifications.shippedTo("Germany").isSatisfiedBy(shipped)).isFalse();
    }
}
