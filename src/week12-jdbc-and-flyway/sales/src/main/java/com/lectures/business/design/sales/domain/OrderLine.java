package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record OrderLine(int productId, Money unitPrice, int quantity, BigDecimal discount) {

    private static final int DISCOUNT_SCALE = 2;

    public OrderLine {
        if (productId <= 0) {
            throw new IllegalArgumentException("productId must be positive: " + productId);
        }
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        Objects.requireNonNull(discount, "discount must not be null");
        if (discount.signum() < 0 || discount.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discount must be within [0, 1]: " + discount);
        }
        try {
            discount = discount.setScale(DISCOUNT_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("discount has more than " + DISCOUNT_SCALE
                    + " decimal places: " + discount, e);
        }
    }

    public Money lineTotal() {
        return unitPrice.times(quantity).discountedBy(discount);
    }

    public OrderLine withAdditionalQuantity(int extra) {
        return new OrderLine(productId, unitPrice, quantity + extra, discount);
    }
}
