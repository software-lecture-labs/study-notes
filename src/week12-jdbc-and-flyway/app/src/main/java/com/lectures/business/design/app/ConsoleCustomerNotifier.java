package com.lectures.business.design.app;

import com.lectures.business.design.sales.application.CustomerNotifier;

final class ConsoleCustomerNotifier implements CustomerNotifier {

    @Override
    public void notify(String customerId, String message) {
        System.out.println("  [notify " + customerId + "] " + message);
    }
}
