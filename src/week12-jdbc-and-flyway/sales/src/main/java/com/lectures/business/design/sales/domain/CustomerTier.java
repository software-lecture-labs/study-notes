package com.lectures.business.design.sales.domain;

import java.math.BigDecimal;

public enum CustomerTier implements DiscountPolicy {
    STANDARD(BigDecimal.ZERO),
    SILVER(new BigDecimal("0.05")),
    GOLD(new BigDecimal("0.10"));

    private final BigDecimal rate;

    CustomerTier(BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public BigDecimal rateFor(Order order) {
        return rate;
    }
}
