package com.lectures.business.design.sales.api;

import java.math.BigDecimal;

public record CustomerRevenue(String customerId, BigDecimal revenue, String currency) {

}
