package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.OrderCancelled;
import com.lectures.business.design.sales.domain.OrderConfirmed;
import com.lectures.business.design.sales.domain.OrderEvent;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderStatus;
import com.lectures.business.design.shared.kernel.DomainEvent;
import com.lectures.business.design.shared.kernel.Money;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InProcessEventPublisherTest {

    private static final OrderConfirmed CONFIRMED
            = new OrderConfirmed(OrderId.of(10248), CustomerId.of("VINET"), Money.tl("140.00"));
    private static final OrderCancelled CANCELLED
            = new OrderCancelled(OrderId.of(10249), CustomerId.of("VINET"), OrderStatus.DRAFT);

    @Test
    @DisplayName("an observer hears its own type and every subtype of it")
    void deliveryFollowsTheTypeHierarchy() {
        List<String> heard = new ArrayList<>();
        InProcessEventPublisher publisher = new InProcessEventPublisher()
                .subscribe(OrderConfirmed.class, e -> heard.add("confirmed-only"))
                .subscribe(OrderEvent.class, e -> heard.add("any-order-event"))
                .subscribe(DomainEvent.class, e -> heard.add("any-domain-event"));

        publisher.publish(CONFIRMED);
        publisher.publish(CANCELLED);

        assertThat(heard).containsExactly(
                "confirmed-only", "any-order-event", "any-domain-event",
                "any-order-event", "any-domain-event");
    }

    @Test
    @DisplayName("nobody listening is not an error")
    void publishingWithoutObserversIsANoOp() {
        new InProcessEventPublisher().publishAll(List.of(CONFIRMED, CANCELLED));
    }

    @Test
    @DisplayName("synchronous dispatch: a failing observer stops the ones after it")
    void aFailingObserverPropagates() {
        List<String> heard = new ArrayList<>();
        InProcessEventPublisher publisher = new InProcessEventPublisher()
                .subscribe(OrderEvent.class, e -> {
                    throw new IllegalStateException("mail server down");
                })
                .subscribe(OrderEvent.class, e -> heard.add("never reached"));

        assertThatThrownBy(() -> publisher.publish(CONFIRMED))
                .hasMessage("mail server down");
        assertThat(heard).isEmpty();
    }
}
