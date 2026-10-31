package com.lectures.business.design.sales.application;

import com.lectures.business.design.shared.kernel.DomainEvent;
import java.util.List;

@FunctionalInterface
public interface DomainEventPublisher {

    void publish(DomainEvent event);

    default void publishAll(List<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
