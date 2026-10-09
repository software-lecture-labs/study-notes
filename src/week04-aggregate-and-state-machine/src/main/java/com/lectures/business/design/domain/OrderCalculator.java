package com.lectures.business.design.domain;

public final class OrderCalculator {

    public double total(AnemicOrder order) {
        double sum = 0;
        for (AnemicOrderLine line : order.getLines()) {
            sum += line.getUnitPrice() * line.getQuantity() * (1 - line.getDiscount());
        }
        return sum;
    }
}
