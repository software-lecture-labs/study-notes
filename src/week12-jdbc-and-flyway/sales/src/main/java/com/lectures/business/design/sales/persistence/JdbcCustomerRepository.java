package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.shared.kernel.Address;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcCustomerRepository implements CustomerRepository {

    private static final String UPDATE_CUSTOMER = """
            UPDATE customers
               SET company_name = ?, contact_name = ?, street = ?, city = ?, postal_code = ?, country = ?
             WHERE customer_id = ?
            """;
    private static final String INSERT_CUSTOMER = """
            INSERT INTO customers (company_name, contact_name, street, city, postal_code, country, customer_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT_CUSTOMER = """
            SELECT customer_id, company_name, contact_name, street, city, postal_code, country
              FROM customers
             WHERE customer_id = ?
            """;

    private final DataSource dataSource;

    public JdbcCustomerRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public void save(Customer customer) {
        Objects.requireNonNull(customer, "customer must not be null");
        try (Connection connection = dataSource.getConnection()) {
            if (writeCustomer(connection, UPDATE_CUSTOMER, customer) == 0) {
                writeCustomer(connection, INSERT_CUSTOMER, customer);
            }
        } catch (SQLException e) {
            throw new RepositoryException("could not save customer " + customer.customerId(), e);
        }
    }

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_CUSTOMER)) {
            statement.setString(1, customerId.value());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(toCustomer(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("could not load customer " + customerId, e);
        }
    }

    private static int writeCustomer(Connection connection, String sql, Customer customer) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Address address = customer.address();
            statement.setString(1, customer.companyName());
            statement.setString(2, customer.contactName().orElse(null));
            statement.setString(3, address.street());
            statement.setString(4, address.city());
            statement.setString(5, address.postalCode());
            statement.setString(6, address.country());
            statement.setString(7, customer.customerId().value());
            return statement.executeUpdate();
        }
    }

    private static Customer toCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
                CustomerId.of(rs.getString("customer_id")),
                rs.getString("company_name"),
                new Address(rs.getString("street"), rs.getString("city"),
                        rs.getString("postal_code"), rs.getString("country")));
        String contact = rs.getString("contact_name");
        if (contact != null) {
            customer.assignContact(contact);
        }
        return customer;
    }
}
