package com.lectures.business.design.app.anemic;

/**
 * The behaviour the anemic order does not have, moved into a separate
 * "service". Nothing guarantees that every caller uses it, or uses it the same
 * way.
 */
public final class OrderCalculator {

    public double total(AnemicOrder order) {
        double sum = 0;
        for (AnemicOrderLine line : order.getLines()) {
            sum += line.getUnitPrice() * line.getQuantity() * (1 - line.getDiscount());
        }
        return sum;
    }
}
