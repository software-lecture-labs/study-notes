package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.CustomerNotFoundException;
import com.lectures.business.design.shared.kernel.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryCustomerRepositoryTest {

    @Test
    @DisplayName("customers are found by their typed id")
    void findAndGet() {
        CustomerRepository repository = new InMemoryCustomerRepository();
        repository.save(new Customer(CustomerId.of("TOMSP"), "Toms Spezialitaten",
                new Address("Luisenstr. 48", "Munster", "44087", "Germany")));

        assertThat(repository.findById(CustomerId.of("tomsp"))).isPresent();
        assertThat(repository.findById(CustomerId.of("ALFKI"))).isEmpty();
        assertThatThrownBy(() -> repository.getById(CustomerId.of("ALFKI")))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}
