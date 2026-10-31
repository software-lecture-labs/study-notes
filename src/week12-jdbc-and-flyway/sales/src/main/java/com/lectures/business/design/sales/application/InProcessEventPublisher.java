package com.lectures.business.design.sales.application;

import com.lectures.business.design.shared.kernel.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class InProcessEventPublisher implements DomainEventPublisher {

    private record Subscription<E extends DomainEvent>(Class<E> type, Consumer<? super E> handler) {

        void deliver(DomainEvent event) {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        }
    }

    private final List<Subscription<?>> subscriptions = new ArrayList<>();

    public <E extends DomainEvent> InProcessEventPublisher subscribe(Class<E> type, Consumer<? super E> handler) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        subscriptions.add(new Subscription<>(type, handler));
        return this;
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        for (Subscription<?> subscription : subscriptions) {
            subscription.deliver(event);
        }
    }
}
