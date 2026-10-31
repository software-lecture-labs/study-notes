package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderLine;
import com.lectures.business.design.sales.domain.OrderStatus;
import com.lectures.business.design.shared.kernel.Address;
import com.lectures.business.design.shared.kernel.Money;
import com.lectures.business.design.shared.kernel.Specification;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcOrderRepository implements OrderRepository {

    private static final String ORDER_COLUMNS = """
            order_id, customer_id, order_date, status,
            ship_street, ship_city, ship_postal_code, ship_country, shipped_date
            """;
    private static final String LINE_COLUMNS
            = "order_id, line_no, product_id, unit_price, currency, quantity, discount";

    private static final String UPDATE_ORDER = """
            UPDATE orders
               SET customer_id = ?, order_date = ?, status = ?,
                   ship_street = ?, ship_city = ?, ship_postal_code = ?, ship_country = ?,
                   shipped_date = ?
             WHERE order_id = ?
            """;
    private static final String INSERT_ORDER = """
            INSERT INTO orders (customer_id, order_date, status,
                                ship_street, ship_city, ship_postal_code, ship_country,
                                shipped_date, order_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String DELETE_LINES = "DELETE FROM order_lines WHERE order_id = ?";
    private static final String INSERT_LINE
            = "INSERT INTO order_lines (" + LINE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final DataSource dataSource;

    public JdbcOrderRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public void save(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (writeOrder(connection, UPDATE_ORDER, order) == 0) {
                    writeOrder(connection, INSERT_ORDER, order);
                }
                replaceLines(connection, order);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositoryException("could not save order " + order.orderId(), e);
        }
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        List<Order> found = query(" WHERE order_id = ?", orderId.value());
        return found.stream().findFirst();
    }

    @Override
    public List<Order> findAll() {
        return query("", null);
    }

    @Override
    public List<Order> findAll(Specification<Order> specification) {
        Objects.requireNonNull(specification, "specification must not be null");
        // Honest but expensive: every order is loaded, then filtered in memory.
        // Turning a Specification into a WHERE clause is what JPA Criteria does for us.
        return findAll().stream()
                .filter(specification::isSatisfiedBy)
                .toList();
    }

    @Override
    public long count() {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM orders"); ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RepositoryException("could not count orders", e);
        }
    }

    // --- writing ---
    private static int writeOrder(Connection connection, String sql, Order order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Optional<Address> address = order.shippingAddress();
            statement.setString(1, order.customerId().value());
            statement.setObject(2, order.orderDate());
            statement.setString(3, order.status().name());
            statement.setString(4, address.map(Address::street).orElse(null));
            statement.setString(5, address.map(Address::city).orElse(null));
            statement.setString(6, address.map(Address::postalCode).orElse(null));
            statement.setString(7, address.map(Address::country).orElse(null));
            statement.setObject(8, order.shippedDate().orElse(null));
            statement.setInt(9, order.orderId().value());
            return statement.executeUpdate();
        }
    }

    private static void replaceLines(Connection connection, Order order) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(DELETE_LINES)) {
            delete.setInt(1, order.orderId().value());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT_LINE)) {
            int lineNo = 1;
            for (OrderLine line : order.lines()) {
                insert.setInt(1, order.orderId().value());
                insert.setInt(2, lineNo++);
                insert.setInt(3, line.productId());
                insert.setBigDecimal(4, line.unitPrice().amount());
                insert.setString(5, line.unitPrice().currency().getCurrencyCode());
                insert.setInt(6, line.quantity());
                insert.setBigDecimal(7, line.discount());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private List<Order> query(String where, Integer orderId) {
        String orderSql = "SELECT " + ORDER_COLUMNS + " FROM orders" + where + " ORDER BY order_id";
        String lineSql = "SELECT " + LINE_COLUMNS + " FROM order_lines" + where + " ORDER BY order_id, line_no";

        try (Connection connection = dataSource.getConnection()) {
            Map<Integer, List<OrderLine>> lines = readLines(connection, lineSql, orderId);
            List<Order> orders = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(orderSql)) {
                if (orderId != null) {
                    statement.setInt(1, orderId);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        orders.add(toOrder(rs, lines.getOrDefault(rs.getInt("order_id"), List.of())));
                    }
                }
            }
            return List.copyOf(orders);
        } catch (SQLException e) {
            throw new RepositoryException("could not load orders", e);
        }
    }

    private static Map<Integer, List<OrderLine>> readLines(Connection connection, String sql, Integer orderId)
            throws SQLException {
        Map<Integer, List<OrderLine>> lines = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (orderId != null) {
                statement.setInt(1, orderId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    lines.computeIfAbsent(rs.getInt("order_id"), id -> new ArrayList<>())
                            .add(new OrderLine(
                                    rs.getInt("product_id"),
                                    new Money(rs.getBigDecimal("unit_price"),
                                            Currency.getInstance(rs.getString("currency"))),
                                    rs.getInt("quantity"),
                                    rs.getBigDecimal("discount")));
                }
            }
        }
        return lines;
    }

    private static Order toOrder(ResultSet rs, List<OrderLine> lines) throws SQLException {
        String street = rs.getString("ship_street");
        Address address = street == null ? null : new Address(street, rs.getString("ship_city"),
                rs.getString("ship_postal_code"), rs.getString("ship_country"));

        return Order.reconstitute(
                OrderId.of(rs.getInt("order_id")),
                CustomerId.of(rs.getString("customer_id")),
                rs.getObject("order_date", LocalDate.class),
                OrderStatus.valueOf(rs.getString("status")),
                address,
                rs.getObject("shipped_date", LocalDate.class),
                lines);
    }
}
