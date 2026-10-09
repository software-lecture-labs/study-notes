
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lectures.business.design.domain.Address;
import com.lectures.business.design.domain.CustomerId;
import com.lectures.business.design.domain.Money;
import com.lectures.business.design.domain.Order;
import com.lectures.business.design.domain.OrderBook;
import com.lectures.business.design.domain.OrderId;
import com.lectures.business.design.domain.OrderNotFoundException;
import com.lectures.business.design.domain.OrderStatus;

class OrderBookTest {

    private static final BigDecimal NO_DISCOUNT = BigDecimal.ZERO;
    private static final Address REIMS
            = new Address("59 rue de l'Abbaye", "Reims", "51100", "France");

    private OrderBook book;

    private Order draft(OrderId orderId, CustomerId customerId, String unitPrice, int quantity) {
        Order order = new Order(orderId, customerId, LocalDate.of(1996, 7, 4));
        order.addLine(11, Money.tl(unitPrice), quantity, NO_DISCOUNT);
        order.shipTo(REIMS);
        return order;
    }

    @BeforeEach
    void setUp() {
        book = new OrderBook();
        Order shipped = draft(OrderId.of(10248), CustomerId.of("VINET"), "14.00", 10);   // 140.00
        shipped.confirm();
        shipped.ship(LocalDate.of(1996, 7, 16));
        Order confirmed = draft(OrderId.of(10249), CustomerId.of("VINET"), "10.00", 3);  // 30.00
        confirmed.confirm();
        Order cancelled = draft(OrderId.of(10250), CustomerId.of("TOMSP"), "99.00", 5);  // 495.00, sayılmayacak
        cancelled.cancel();
        Order open = draft(OrderId.of(10251), CustomerId.of("TOMSP"), "20.00", 4);       // 80.00
        book.add(shipped);
        book.add(confirmed);
        book.add(cancelled);
        book.add(open);
    }

    @Test

    @DisplayName("findById returns the order, or empty — it never returns null")
    void findByIdIsOptional() {
        assertThat(book.findById(OrderId.of(10248))).isPresent();
        assertThat(book.findById(OrderId.of(99999))).isEmpty();
    }

    @Test
    @DisplayName("getById throws a domain exception carrying the id")
    void getByIdThrowsWithTheId() {
        assertThat(book.getById(OrderId.of(10248)).customerId()).isEqualTo(CustomerId.of("VINET"));
        assertThatThrownBy(() -> book.getById(OrderId.of(99999)))
                .isInstanceOf(OrderNotFoundException.class)
                .extracting(e -> ((OrderNotFoundException) e).orderId())
                .isEqualTo(OrderId.of(99999));
    }

    @Test
    @DisplayName("the same order cannot be added twice")
    void duplicatesAreRejected() {
        assertThatThrownBy(() -> book.add(draft(OrderId.of(10248), CustomerId.of("VINET"), "1.00", 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(book.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("findByCustomer answers the question Customer refused to store")
    void findByCustomerReturnsOnlyThatCustomer() {
        assertThat(book.findByCustomer(CustomerId.of("VINET")))
                .extracting(Order::orderId)
                .containsExactly(OrderId.of(10248), OrderId.of(10249));
        assertThatThrownBy(() -> book.findByCustomer(CustomerId.of("NOBODY")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the returned collections are immutable")
    void returnedCollectionsAreImmutable() {
        assertThat(book.findByCustomer(CustomerId.of("VINET"))).isUnmodifiable();
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
                .containsEntry(CustomerId.of("VINET"), Money.tl("170.00"))
                .containsEntry(CustomerId.of("TOMSP"), Money.tl("80.00"));
        assertThat(book.totalRevenue()).isEqualTo(Money.tl("250.00"));
    }

    @Test
    @DisplayName("topCustomers ranks by revenue, highest first")
    void topCustomersAreRanked() {
        assertThat(book.topCustomers(1)).containsExactly(CustomerId.of("VINET"));
        assertThat(book.topCustomers(5)).containsExactly(CustomerId.of("VINET"), CustomerId.of("TOMSP"));
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
