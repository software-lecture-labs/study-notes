package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.OrderShipped;
import java.util.Objects;
import java.util.function.Consumer;

public final class ShipmentNotificationHandler implements Consumer<OrderShipped> {

    private final CustomerNotifier notifier;

    public ShipmentNotificationHandler(CustomerNotifier notifier) {
        this.notifier = Objects.requireNonNull(notifier, "notifier must not be null");
    }

    @Override
    public void accept(OrderShipped event) {
        notifier.notify(event.customerId().value(),
                "Order " + event.orderId() + " shipped on " + event.shippedDate()
                + " to " + event.destination().city());
    }
}
