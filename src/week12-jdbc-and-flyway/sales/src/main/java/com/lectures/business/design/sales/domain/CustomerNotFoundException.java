package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.DomainException;

public class CustomerNotFoundException extends DomainException {

    private final CustomerId customerId;

    public CustomerNotFoundException(CustomerId customerId) {
        super("no customer with id " + customerId);
        this.customerId = customerId;
    }

    public CustomerId customerId() {
        return customerId;
    }
}
