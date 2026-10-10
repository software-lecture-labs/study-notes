package com.lectures.business.design.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    @Test
    @DisplayName("a draft can be confirmed or cancelled, nothing else")
    void draftTransitions() {
        assertThat(OrderStatus.DRAFT.allowedTransitions())
                .containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("a draft cannot be shipped without being confirmed first")
    void draftCannotSkipConfirmation() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
    }

    @Test
    @DisplayName("transitions are one-way: no status leads back to DRAFT")
    void transitionsAreOneWay() {
        for (OrderStatus status : OrderStatus.values()) {
            assertThat(status.canTransitionTo(OrderStatus.DRAFT)).isFalse();
        }
    }

    @Test
    @DisplayName("shipped and cancelled are terminal, and only a draft is modifiable")
    void terminalAndModifiableStates() {
        assertThat(OrderStatus.SHIPPED.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(OrderStatus.DRAFT.isTerminal()).isFalse();
        assertThat(OrderStatus.CONFIRMED.isTerminal()).isFalse();

        assertThat(OrderStatus.DRAFT.isModifiable()).isTrue();
        assertThat(OrderStatus.CONFIRMED.isModifiable()).isFalse();
        assertThat(OrderStatus.SHIPPED.isModifiable()).isFalse();
    }

    @Test
    @DisplayName("the transition table handed out cannot be modified")
    void transitionTableIsUnmodifiable() {
        assertThatThrownBy(() -> OrderStatus.DRAFT.allowedTransitions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
