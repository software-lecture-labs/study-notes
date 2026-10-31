package com.lectures.business.design.sales.application;

@FunctionalInterface
public interface CustomerNotifier {

    void notify(String customerId, String message);
}
