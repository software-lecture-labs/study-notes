package com.lectures.business.design.domain;

@FunctionalInterface
public interface OrderIdGenerator {
    OrderId next();
}
