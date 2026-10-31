package com.lectures.business.design.sales.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderIdTest {

    @Test
    @DisplayName("zero and negative numbers are not order ids")
    void positiveOnly() {
        assertThatThrownBy(() -> OrderId.of(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderId.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("order ids are equal by value and ordered by number")
    void equalityAndOrdering() {
        assertThat(OrderId.of(10248)).isEqualTo(OrderId.of(10248));
        assertThat(OrderId.of(10248)).isLessThan(OrderId.of(10249));
    }

    @Test
    @DisplayName("toString prints the number, so log messages stay readable")
    void toStringIsReadable() {
        assertThat(OrderId.of(10248)).hasToString("10248");
    }
}
