package com.lectures.business.design.domain;

import java.util.concurrent.atomic.AtomicInteger;

public final class SequentialOrderIdGenerator implements OrderIdGenerator {

    private final AtomicInteger counter;

    public SequentialOrderIdGenerator(int startingAt) {
        if (startingAt <= 0) {
            throw new IllegalArgumentException("startingAt must be positive: " + startingAt);
        }
        this.counter = new AtomicInteger(startingAt);
    }

    @Override
    public OrderId next() {
        return OrderId.of(counter.getAndIncrement());
    }
}
