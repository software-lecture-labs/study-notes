package com.lectures.business.design.sales.domain;

@FunctionalInterface
public interface OrderIdGenerator {
    OrderId next();
}
