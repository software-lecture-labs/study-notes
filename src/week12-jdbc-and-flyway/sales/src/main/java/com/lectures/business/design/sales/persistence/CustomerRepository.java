package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.CustomerNotFoundException;
import java.util.Optional;

public interface CustomerRepository {

    void save(Customer customer);

    Optional<Customer> findById(CustomerId customerId);

    default Customer getById(CustomerId customerId) {
        return findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
