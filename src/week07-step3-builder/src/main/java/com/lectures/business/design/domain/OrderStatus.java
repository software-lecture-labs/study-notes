package com.lectures.business.design.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    SHIPPED,
    CANCELLED;
    // DELIVERED durumunu bilinçli olarak kaldırdık.

    private Set<OrderStatus> allowedTransitions;

    static {
        DRAFT.allowedTransitions = EnumSet.of(CONFIRMED, CANCELLED);
        CONFIRMED.allowedTransitions = EnumSet.of(SHIPPED, CANCELLED);
        SHIPPED.allowedTransitions = EnumSet.noneOf(OrderStatus.class);
        CANCELLED.allowedTransitions = EnumSet.noneOf(OrderStatus.class);
    }

    public Set<OrderStatus> allowedTransitions() {
        return Collections.unmodifiableSet(allowedTransitions);
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions.contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }

    public boolean isModifiable() {
        return this == DRAFT;
    }
}
