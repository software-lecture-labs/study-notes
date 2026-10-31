package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderBuilder;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderNotFoundException;
import com.lectures.business.design.sales.domain.OrderSpecifications;
import com.lectures.business.design.sales.domain.OrderStatus;
import com.lectures.business.design.shared.kernel.Address;
import com.lectures.business.design.shared.kernel.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class OrderRepositoryContract {

    protected OrderRepository repository;

    protected abstract OrderRepository createRepository();

    @BeforeEach
    void setUpContract() {
        repository = createRepository();
        repository.save(OrderBuilder.anOrder()
                .withId(10248).forCustomer("VINET").withLine(11, "14.00", 10).shipped().build());
        repository.save(OrderBuilder.anOrder()
                .withId(10249).forCustomer("VINET").withLine(11, "10.00", 3).confirmed().build());
        repository.save(OrderBuilder.anOrder()
                .withId(10250).forCustomer("TOMSP").withLine(11, "99.00", 5).cancelled().build());
        repository.save(OrderBuilder.anOrder()
                .withId(10251).forCustomer("TOMSP").withLine(11, "20.00", 4).build());
    }

    @Test
    @DisplayName("findById answers with an Optional, getById with an exception")
    void findByIdAndGetById() {
        assertThat(repository.findById(OrderId.of(10248))).isPresent();
        assertThat(repository.findById(OrderId.of(99999))).isEmpty();
        assertThatThrownBy(() -> repository.getById(OrderId.of(99999)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("an aggregate comes back exactly as it was saved")
    void roundTripKeepsEveryField() {
        Order original = OrderBuilder.anOrder()
                .withId(10260).forCustomer("TOMSP").orderedOn(LocalDate.of(1996, 7, 19))
                .shippingTo(new Address("Luisenstr. 48", "Munster", "44087", "Germany"))
                .withLine(11, "14.00", 12)
                .withDiscountedLine(42, "9.80", 10, "0.1")
                .shipped().build();
        repository.save(original);

        Order loaded = repository.getById(OrderId.of(10260));

        assertThat(loaded.customerId()).isEqualTo(CustomerId.of("TOMSP"));
        assertThat(loaded.orderDate()).isEqualTo(LocalDate.of(1996, 7, 19));
        assertThat(loaded.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(loaded.shippedDate()).isEqualTo(original.shippedDate());
        assertThat(loaded.shippingAddress()).isEqualTo(original.shippingAddress());
        assertThat(loaded.lines()).isEqualTo(original.lines());
        assertThat(loaded.total()).isEqualTo(original.total());
    }

    @Test
    @DisplayName("a change that is not saved is not stored")
    void unsavedChangesAreLost() {
        Order order = repository.getById(OrderId.of(10249));
        order.cancel();

        assertThat(repository.getById(OrderId.of(10249)).status()).isEqualTo(OrderStatus.CONFIRMED);

        repository.save(order);
        assertThat(repository.getById(OrderId.of(10249)).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("save replaces by identity: lines that left the aggregate leave the store")
    void saveIsUpsertOfTheWholeAggregate() {
        Order draft = repository.getById(OrderId.of(10251));
        draft.addLine(42, Money.tl("9.80"), 1, BigDecimal.ZERO);
        draft.removeLine(11);
        repository.save(draft);

        assertThat(repository.count()).isEqualTo(4);
        assertThat(repository.getById(OrderId.of(10251)).productIds()).containsExactly(42);
    }

    @Test
    @DisplayName("a loaded aggregate announces nothing: reconstitution is not a business event")
    void loadingRaisesNoEvents() {
        assertThat(repository.getById(OrderId.of(10248)).pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("findAll returns every aggregate as an unmodifiable list")
    void findAllReturnsEverything() {
        assertThat(repository.findAll())
                .isUnmodifiable()
                .extracting(order -> order.orderId().value())
                .containsExactlyInAnyOrder(10248, 10249, 10250, 10251);
    }

    @Test
    @DisplayName("specifications select and combine, ordered by id")
    void specificationDrivesTheQuery() {
        assertThat(repository.findAll(OrderSpecifications.placedBy(CustomerId.of("VINET"))))
                .extracting(order -> order.orderId().value())
                .containsExactly(10248, 10249);
        assertThat(repository.findAll(OrderSpecifications.placedBy(CustomerId.of("ALFKI")))).isEmpty();
        assertThat(repository.findAll(OrderSpecifications.notCancelled()
                .and(OrderSpecifications.placedBy(CustomerId.of("TOMSP")))))
                .extracting(order -> order.orderId().value())
                .containsExactly(10251);
    }

    @Test
    @DisplayName("an empty repository answers without blowing up")
    void emptyRepository() {
        OrderRepository empty = createRepository();
        assertThat(empty.count()).isZero();
        assertThat(empty.findAll()).isEmpty();
        assertThat(empty.findAll(OrderSpecifications.notCancelled())).isEmpty();
        assertThat(empty.findById(OrderId.of(10248))).isEmpty();
    }
}
