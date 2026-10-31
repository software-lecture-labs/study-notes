package com.lectures.business.design.sales.domain;

import java.math.BigDecimal;

public final class VolumeDiscountPolicy implements DiscountPolicy {

    private static final BigDecimal LARGE = new BigDecimal("0.15");
    private static final BigDecimal MEDIUM = new BigDecimal("0.10");
    private static final BigDecimal SMALL = new BigDecimal("0.05");

    @Override
    public BigDecimal rateFor(Order order) {
        int quantity = order.totalQuantity();
        if (quantity >= 100) {
            return LARGE;
        }

        if (quantity >= 50) {
            return MEDIUM;
        }

        if (quantity >= 20) {
            return SMALL;
        }
        return BigDecimal.ZERO;
    }
}
