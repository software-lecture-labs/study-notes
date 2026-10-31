package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.OrderCancelled;
import com.lectures.business.design.sales.domain.OrderConfirmed;
import com.lectures.business.design.sales.domain.OrderEvent;
import com.lectures.business.design.sales.domain.OrderShipped;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class OrderActivityLog implements Consumer<OrderEvent> {

    private final List<String> entries = new ArrayList<>();

    @Override
    public void accept(OrderEvent event) {
        entries.add(describe(event));
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    static String describe(OrderEvent event) {
        return switch (event) {
            case OrderConfirmed confirmed ->
                confirmed.orderId() + " confirmed, total " + confirmed.total();
            case OrderShipped shipped ->
                shipped.orderId() + " shipped on " + shipped.shippedDate();
            case OrderCancelled cancelled ->
                cancelled.orderId() + " cancelled while " + cancelled.previousStatus();
        };
    }
}
