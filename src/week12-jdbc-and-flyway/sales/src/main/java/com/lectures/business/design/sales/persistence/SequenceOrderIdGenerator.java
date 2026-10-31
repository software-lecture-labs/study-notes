package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderIdGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

public final class SequenceOrderIdGenerator implements OrderIdGenerator {

    private final DataSource dataSource;

    public SequenceOrderIdGenerator(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public OrderId next() {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT nextval('orders_seq')"); ResultSet rs = statement.executeQuery()) {
            rs.next();
            return OrderId.of(rs.getInt(1));
        } catch (SQLException e) {
            throw new RepositoryException("could not draw the next order id", e);
        }
    }
}
