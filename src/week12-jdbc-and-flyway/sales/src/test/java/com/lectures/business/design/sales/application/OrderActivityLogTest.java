package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.OrderCancelled;
import com.lectures.business.design.sales.domain.OrderConfirmed;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderStatus;
import com.lectures.business.design.shared.kernel.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrderActivityLogTest {

    @Test
    @DisplayName("the log describes every kind of order event")
    void describesEachEvent() {
        OrderActivityLog log = new OrderActivityLog();
        log.accept(new OrderConfirmed(OrderId.of(10248), CustomerId.of("VINET"), Money.tl("140.00")));
        log.accept(new OrderCancelled(OrderId.of(10248), CustomerId.of("VINET"), OrderStatus.CONFIRMED));

        assertThat(log.entries()).containsExactly(
                "10248 confirmed, total 140.00 TRY",
                "10248 cancelled while CONFIRMED");
    }
}
