package com.lectures.business.design.sales.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.OrderBuilder;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.shared.kernel.Address;

class JdbcOrderRepositoryTest extends OrderRepositoryContract {

    private DataSource dataSource;

    static void seedCustomers(DataSource dataSource) {
        CustomerRepository customers = new JdbcCustomerRepository(dataSource);
        customers.save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));
        customers.save(new Customer(CustomerId.of("TOMSP"), "Toms Spezialitaten",
                new Address("Luisenstr. 48", "Munster", "44087", "Germany")));
    }

    @Override
    protected OrderRepository createRepository() {
        dataSource = TestDatabases.h2();
        seedCustomers(dataSource);
        return new JdbcOrderRepository(dataSource);
    }

    @Test
    @DisplayName("the foreign key refuses an order for a customer that does not exist")
    void unknownCustomerIsRejectedByTheDatabase() {
        assertThatThrownBy(() -> repository.save(OrderBuilder.anOrder()
                .withId(10300).forCustomer("ALFKI").withLine(11, "14.00", 1).build()))
                .isInstanceOf(RepositoryException.class)
                .hasRootCauseInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("a failed save leaves no half-written aggregate behind")
    void saveIsAtomic() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE order_lines ADD CONSTRAINT ck_sabotage CHECK (quantity <> 7)");
        }

        assertThatThrownBy(() -> repository.save(OrderBuilder.anOrder()
                .withId(10301).forCustomer("VINET").withLine(11, "14.00", 7).build()))
                .isInstanceOf(RepositoryException.class);
        assertThat(repository.findById(OrderId.of(10301))).isEmpty();
        assertThat(repository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("ids come from the database sequence and survive a restart")
    void sequenceHandsOutIds() {
        SequenceOrderIdGenerator first = new SequenceOrderIdGenerator(dataSource);
        SequenceOrderIdGenerator afterRestart = new SequenceOrderIdGenerator(dataSource);
        assertThat(first.next().value()).isEqualTo(10248);
        assertThat(afterRestart.next().value()).isEqualTo(10249);
    }
}
