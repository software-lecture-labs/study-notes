package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountPolicyTest {

    private Order orderOf(int quantity) {
        return OrderBuilder.anOrder().withLine(11, "10.00", quantity).build();
    }

    @Test
    @DisplayName("the null object takes nothing off")
    void noneChangesNothing() {
        Order order = orderOf(30);
        assertThat(order.totalWith(DiscountPolicy.NONE)).isEqualTo(order.total());
    }

    @Test
    @DisplayName("volume discount grows with the number of units")
    void volumeDiscountHasTiers() {
        VolumeDiscountPolicy policy = new VolumeDiscountPolicy();
        assertThat(orderOf(10).totalWith(policy)).isEqualTo(Money.tl("100.00"));
        assertThat(orderOf(30).totalWith(policy)).isEqualTo(Money.tl("285.00"));
        assertThat(orderOf(60).totalWith(policy)).isEqualTo(Money.tl("540.00"));
        assertThat(orderOf(120).totalWith(policy)).isEqualTo(Money.tl("1020.00"));
    }

    @Test
    @DisplayName("an enum can be a strategy")
    void tiersAreStrategies() {
        Order order = orderOf(30);
        assertThat(order.totalWith(CustomerTier.STANDARD)).isEqualTo(Money.tl("300.00"));
        assertThat(order.totalWith(CustomerTier.SILVER)).isEqualTo(Money.tl("285.00"));
        assertThat(order.totalWith(CustomerTier.GOLD)).isEqualTo(Money.tl("270.00"));
    }

    @Test
    @DisplayName("the same order can be priced under two policies")
    void theOrderRemembersNoPolicy() {
        Order order = orderOf(30);
        assertThat(order.totalWith(CustomerTier.GOLD)).isEqualTo(Money.tl("270.00"));
        assertThat(order.totalWith(CustomerTier.STANDARD)).isEqualTo(Money.tl("300.00"));
        assertThat(order.total()).isEqualTo(Money.tl("300.00"));
    }

    @Test
    @DisplayName("a null policy is rejected, not silently ignored")
    void nullPolicyIsRejected() {
        assertThatThrownBy(() -> orderOf(1).totalWith(null))
                .isInstanceOf(NullPointerException.class);
    }
}
