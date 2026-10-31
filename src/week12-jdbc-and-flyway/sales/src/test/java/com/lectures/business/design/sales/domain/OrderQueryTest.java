package com.lectures.business.design.sales.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderQueryTest {

    private Order threeLines() {
        return OrderBuilder.anOrder()
                .withLine(72, "34.80", 5)
                .withLine(11, "14.00", 10)
                .withDiscountedLine(42, "9.80", 10, "0.15")
                .build();
    }

    private Order noLines() {
        return OrderBuilder.anOrder().build();
    }

    @Test
    @DisplayName("lineFor finds a line by product, or returns empty")
    void lineForIsOptional() {
        Order order = threeLines();
        assertThat(order.lineFor(42))
                .hasValueSatisfying(line -> assertThat(line.quantity()).isEqualTo(10));
        assertThat(order.lineFor(99)).isEmpty();
    }

    @Test
    @DisplayName("totalQuantity sums the quantities of every line")
    void totalQuantitySumsLines() {
        assertThat(threeLines().totalQuantity()).isEqualTo(25);
        assertThat(noLines().totalQuantity()).isZero();
    }

    @Test
    @DisplayName("a product added twice is counted once, with the merged quantity")
    void mergedLinesAreCountedOnce() {
        Order order = OrderBuilder.anOrder()
                .withLine(11, "14.00", 5)
                .withLine(11, "14.00", 7)
                .build();
        assertThat(order.productIds()).containsExactly(11);
        assertThat(order.totalQuantity()).isEqualTo(12);
    }

    @Test
    @DisplayName("hasDiscountedLine is true only when a line has a positive discount")
    void discountedLineDetection() {
        assertThat(threeLines().hasDiscountedLine()).isTrue();
        assertThat(OrderBuilder.anOrder().withLine(11, "14.00", 10).build().hasDiscountedLine())
                .isFalse();
        assertThat(noLines().hasDiscountedLine()).isFalse();
    }

    @Test
    @DisplayName("mostValuableLine picks the highest line total")
    void mostValuableLineByTotal() {
        assertThat(threeLines().mostValuableLine())
                .map(OrderLine::productId)
                .contains(72);
    }

    @Test
    @DisplayName("mostValuableLine compares totals after the discount, not unit prices")
    void mostValuableLineUsesDiscountedTotals() {
        Order order = OrderBuilder.anOrder()
                .withDiscountedLine(72, "34.80", 5, "0.50")
                .withLine(11, "14.00", 10)
                .build();
        assertThat(order.mostValuableLine())
                .map(OrderLine::productId)
                .contains(11);
    }

    @Test
    @DisplayName("an order without lines has no most valuable line")
    void emptyOrderHasNoMostValuableLine() {
        assertThat(noLines().mostValuableLine()).isEmpty();
    }

    @Test
    @DisplayName("productIds are sorted, whatever order the lines were added in")
    void productIdsAreSorted() {
        assertThat(threeLines().productIds()).containsExactly(11, 42, 72);
    }

    @Test
    @DisplayName("productIds is a read-only snapshot")
    void productIdsCannotBeModified() {
        Order order = threeLines();
        assertThatThrownBy(() -> order.productIds().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(order.productIds()).hasSize(3);
    }

    @Test
    @DisplayName("queries do not change the order")
    void queriesHaveNoSideEffects() {
        Order order = threeLines();
        var statusBefore = order.status();
        var linesBefore = order.lines();

        order.lineFor(11);
        order.totalQuantity();
        order.hasDiscountedLine();
        order.mostValuableLine();
        order.productIds();

        assertThat(order.status()).isEqualTo(statusBefore);
        assertThat(order.lines()).isEqualTo(linesBefore);
    }
}
