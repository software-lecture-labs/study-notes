package com.lectures.business.design.domain;

import com.lectures.business.design.domain.Address;
import com.lectures.business.design.domain.Money;
import com.lectures.business.design.domain.Order;
import com.lectures.business.design.domain.OrderBook;
import com.lectures.business.design.domain.OrderNotFoundException;
import com.lectures.business.design.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderBookTest {

    private static final BigDecimal NO_DISCOUNT = BigDecimal.ZERO;
    private static final Address REIMS
            = new Address("59 rue de l'Abbaye", "Reims", "51100", "France");

    private OrderBook book;

    private Order draft(int orderId, String customerId, String unitPrice, int quantity) {
        Order order = new Order(orderId, customerId, LocalDate.of(1996, 7, 4));
        order.addLine(11, Money.tl(unitPrice), quantity, NO_DISCOUNT);
        order.shipTo(REIMS);
        return order;
    }

    @BeforeEach
    void setUp() {
        book = new OrderBook();
        Order shipped = draft(10248, "VINET", "14.00", 10);   // 140.00
        shipped.confirm();
        shipped.ship(LocalDate.of(1996, 7, 16));
        Order confirmed = draft(10249, "VINET", "10.00", 3);  // 30.00
        confirmed.confirm();
        Order cancelled = draft(10250, "TOMSP", "99.00", 5);  // 495.00, sayılmayacak
        cancelled.cancel();
        Order open = draft(10251, "TOMSP", "20.00", 4);       // 80.00
        book.add(shipped);
        book.add(confirmed);
        book.add(cancelled);
        book.add(open);
    }

    @Test
    @DisplayName("findById returns the order, or empty — it never returns null")
    void findByIdIsOptional() {
        assertThat(book.findById(10248)).isPresent();
        assertThat(book.findById(99999)).isEmpty();
    }

    @Test
    @DisplayName("getById throws a domain exception carrying the id")
    void getByIdThrowsWithTheId() {
        assertThat(book.getById(10248).customerId()).isEqualTo("VINET");
        assertThatThrownBy(() -> book.getById(99999))
                .isInstanceOf(OrderNotFoundException.class)
                .extracting(e -> ((OrderNotFoundException) e).orderId())
                .isEqualTo(99999);
    }

    @Test
    @DisplayName("the same order cannot be added twice")
    void duplicatesAreRejected() {
        assertThatThrownBy(() -> book.add(draft(10248, "VINET", "1.00", 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(book.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("findByCustomer answers the question Customer refused to store")
    void findByCustomerReturnsOnlyThatCustomer() {
        assertThat(book.findByCustomer("VINET"))
                .extracting(Order::orderId)
                .containsExactly(10248, 10249);
        assertThat(book.findByCustomer("NOBODY")).isEmpty();
    }

    @Test
    @DisplayName("the returned collections are immutable")
    void returnedCollectionsAreImmutable() {
        assertThat(book.findByCustomer("VINET")).isUnmodifiable();
        assertThat(book.findByStatus(OrderStatus.DRAFT)).isUnmodifiable();
    }

    @Test
    @DisplayName("countByStatus omits statuses that do not occur")
    void countByStatusOmitsMissingKeys() {
        assertThat(book.countByStatus())
                .containsEntry(OrderStatus.SHIPPED, 1L)
                .containsEntry(OrderStatus.CONFIRMED, 1L)
                .containsEntry(OrderStatus.CANCELLED, 1L)
                .containsEntry(OrderStatus.DRAFT, 1L);
        OrderBook empty = new OrderBook();

        assertThat(empty.countByStatus()).doesNotContainKey(OrderStatus.SHIPPED);
        assertThat(empty.countByStatus().getOrDefault(OrderStatus.SHIPPED, 0L)).isZero();
    }

    @Test
    @DisplayName("cancelled orders earn nothing")
    void revenueIgnoresCancelledOrders() {
        assertThat(book.revenueByCustomer())
                .containsEntry("VINET", Money.tl("170.00"))
                .containsEntry("TOMSP", Money.tl("80.00"));
        assertThat(book.totalRevenue()).isEqualTo(Money.tl("250.00"));
    }

    @Test
    @DisplayName("topCustomers ranks by revenue, highest first")
    void topCustomersAreRanked() {
        assertThat(book.topCustomers(1)).containsExactly("VINET");
        assertThat(book.topCustomers(5)).containsExactly("VINET", "TOMSP");
    }

    @Test
    @DisplayName("partitioningBy always produces both keys, even when one side is empty")
    void partitioningAlwaysHasBothKeys() {
        assertThat(book.partitionByShipped().get(true)).hasSize(1);
        assertThat(book.partitionByShipped().get(false)).hasSize(3);

        OrderBook empty = new OrderBook();
        assertThat(empty.partitionByShipped()).containsOnlyKeys(true, false);
        assertThat(empty.partitionByShipped().get(true)).isEmpty();
    }

    @Test
    @DisplayName("an empty book has zero revenue, not a missing one")
    void emptyBookHasZeroRevenue() {
        assertThat(new OrderBook().totalRevenue()).isEqualTo(Money.tl("0"));
    }
}
