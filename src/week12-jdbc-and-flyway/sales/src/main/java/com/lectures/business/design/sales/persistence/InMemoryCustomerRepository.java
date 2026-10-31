package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<CustomerId, Customer> customers = new HashMap<>();

    @Override
    public void save(Customer customer) {
        Objects.requireNonNull(customer, "customer must not be null");
        customers.put(customer.customerId(), customer);
    }

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }
}
