package com.lectures.business.design.domain;

import com.lectures.business.design.domain.IncompleteOrderException.MissingPart;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    private static final BigDecimal NO_DISCOUNT = BigDecimal.ZERO;
    private static final LocalDate ORDER_DATE = LocalDate.of(1996, 7, 4);
    private static final Address DESTINATION =
            new Address("59 rue de l'Abbaye", "Reims", "51100", "France");
    private Order newDraft() {
        return new Order(10248, "VINET", ORDER_DATE);
    }

    private Order newConfirmableDraft() {
        Order order = newDraft();
        order.addLine(11, Money.tl("14.00"), 12, NO_DISCOUNT);
        order.shipTo(DESTINATION);
        return order;
    }

    private Order newShippedOrder() {
        Order order = newConfirmableDraft();
        order.confirm();
        order.ship(ORDER_DATE.plusDays(12));
        return order;
    }

    @Test
    @DisplayName("total sums the lines and applies discounts")
    void totalSumsLines() {
        Order order = newDraft();
        order.addLine(11, Money.tl("14.00"), 12, NO_DISCOUNT);
        order.addLine(42, Money.tl("9.80"), 10, new BigDecimal("0.15"));
        assertThat(order.total()).isEqualTo(Money.tl("251.30"));
    }

    @Test
    @DisplayName("adding the same product twice increases the quantity")
    void sameProductIsMerged() {
        Order order = newDraft();
        order.addLine(11, Money.tl("14.00"), 5, NO_DISCOUNT);
        order.addLine(11, Money.tl("14.00"), 7, NO_DISCOUNT);
        assertThat(order.lines()).hasSize(1);
        assertThat(order.lines().get(0).quantity()).isEqualTo(12);
    }

    @Test
    @DisplayName("the customer is referenced by identity, not by object")
    void customerIsReferencedById() {
        assertThat(newDraft().customerId()).isEqualTo("VINET");
    }

    @Test
    @DisplayName("an empty order reports exactly what is missing")
    void emptyOrderReportsMissingLines() {
        assertThatThrownBy(() -> newDraft().confirm())
                .isInstanceOf(IncompleteOrderException.class)
                .extracting(e -> ((IncompleteOrderException) e).missingPart())
                .isEqualTo(MissingPart.LINES);
    }

    @Test
    @DisplayName("an order without a destination reports exactly what is missing")
    void orderWithoutDestinationReportsMissingAddress() {
        Order order = newDraft();
        order.addLine(11, Money.tl("14.00"), 12, NO_DISCOUNT);
        assertThatThrownBy(order::confirm)
                .isInstanceOf(IncompleteOrderException.class)
                .extracting(e -> ((IncompleteOrderException) e).missingPart())
                .isEqualTo(MissingPart.SHIPPING_ADDRESS);
    }

    @Test
    @DisplayName("the destination is replaced as a whole, never edited in part")
    void destinationIsReplaced() {
        Order order = newConfirmableDraft();
        order.shipTo(new Address("Obere Str. 57", "Berlin", "12209", "Germany"));
        assertThat(order.shippingAddress()).contains(
                new Address("Obere Str. 57", "Berlin", "12209", "Germany"));
    }

    @Test
    @DisplayName("a shipped order rejects new lines and a new destination")
    void shippedOrderIsClosed() {
        Order order = newShippedOrder();
        assertThatThrownBy(() -> order.addLine(42, Money.tl("9.80"), 1, NO_DISCOUNT))
                .isInstanceOf(OrderNotModifiableException.class);
        assertThatThrownBy(() -> order.shipTo(DESTINATION))
                .isInstanceOf(OrderNotModifiableException.class);
        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("a shipped order cannot be cancelled, and the exception says why")
    void shippedOrderCannotBeCancelled() {
        Order order = newShippedOrder();
        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidStateTransitionException.class)
                .satisfies(thrown -> {
                    InvalidStateTransitionException e = (InvalidStateTransitionException) thrown;
                    assertThat(e.from()).isEqualTo(OrderStatus.SHIPPED);
                    assertThat(e.to()).isEqualTo(OrderStatus.CANCELLED);
                    assertThat(e.orderId()).isEqualTo(10248);
                });
    }

    @Test
    @DisplayName("a cancelled order is final: it cannot be confirmed again")
    void cancelledOrderIsFinal() {
        Order order = newConfirmableDraft();
        order.cancel();
        assertThatThrownBy(order::confirm)
                .isInstanceOf(OrderNotModifiableException.class);
        assertThat(order.status().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("the line list handed out is a copy")
    void linesAreDefensivelyCopied() {
        Order order = newConfirmableDraft();
        assertThatThrownBy(() -> order.lines().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(order.lines()).hasSize(1);
    }

    @Test
    @DisplayName("orders are equal by identity, money by value")
    void equalitySemantics() {
        Order a = newDraft();
        Order b = newConfirmableDraft();
        assertThat(a).isEqualTo(b);
        assertThat(Money.tl("18.6")).isEqualTo(Money.tl("18.60"));
    }
}
