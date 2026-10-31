package com.lectures.business.design.sales.api;

import java.util.List;
import java.util.Objects;

public record PlaceOrderCommand(String customerId, List<OrderLineRequest> lines) {

    public PlaceOrderCommand {
        Objects.requireNonNull(lines, "lines must not be null");
        lines = List.copyOf(lines);
    }
}
