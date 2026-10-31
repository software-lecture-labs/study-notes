package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.CustomerNotFoundException;
import com.lectures.business.design.shared.kernel.Address;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerRepositoryContractTest {

    static Stream<Arguments> repositories() {
        return Stream.of(
                Arguments.of("in-memory", (Supplier<CustomerRepository>) InMemoryCustomerRepository::new),
                Arguments.of("jdbc-h2", (Supplier<CustomerRepository>) ()
                        -> new JdbcCustomerRepository(TestDatabases.h2())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("repositories")
    @DisplayName("customers are found by their typed id")
    void findAndGet(String name, Supplier<CustomerRepository> factory) {
        CustomerRepository repository = factory.get();
        repository.save(new Customer(CustomerId.of("TOMSP"), "Toms Spezialitaten",
                new Address("Luisenstr. 48", "Munster", "44087", "Germany")));

        assertThat(repository.findById(CustomerId.of("tomsp"))).isPresent();
        assertThat(repository.findById(CustomerId.of("ALFKI"))).isEmpty();
        assertThatThrownBy(() -> repository.getById(CustomerId.of("ALFKI")))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("repositories")
    @DisplayName("an unsaved relocation is not stored, a saved one is")
    void savingIsExplicit(String name, Supplier<CustomerRepository> factory) {
        CustomerRepository repository = factory.get();
        repository.save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));

        Customer customer = repository.getById(CustomerId.of("VINET"));
        customer.relocateTo(new Address("2 rue du Commerce", "Lyon", "69004", "France"));
        customer.assignContact("Paul Henriot");
        assertThat(repository.getById(CustomerId.of("VINET")).address().city()).isEqualTo("Reims");

        repository.save(customer);
        Customer reloaded = repository.getById(CustomerId.of("VINET"));
        assertThat(reloaded.address().city()).isEqualTo("Lyon");
        assertThat(reloaded.contactName()).contains("Paul Henriot");
    }
}
