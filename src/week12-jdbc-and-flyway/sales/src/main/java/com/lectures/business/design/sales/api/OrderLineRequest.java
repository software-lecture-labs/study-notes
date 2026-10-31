package com.lectures.business.design.sales.api;

import java.math.BigDecimal;

public record OrderLineRequest(int productId, BigDecimal unitPrice, int quantity, BigDecimal discount) {

    public static OrderLineRequest of(int productId, String unitPrice, int quantity) {
        return new OrderLineRequest(productId, new BigDecimal(unitPrice), quantity, BigDecimal.ZERO);
    }
}
