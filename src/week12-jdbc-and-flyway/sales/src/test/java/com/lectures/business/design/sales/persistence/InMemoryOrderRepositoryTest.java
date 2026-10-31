package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderBuilder;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderNotFoundException;
import com.lectures.business.design.sales.domain.OrderSpecifications;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {

    private OrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
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
    @DisplayName("save replaces by identity instead of rejecting a second write")
    void saveIsUpsert() {
        Order replacement = OrderBuilder.anOrder()
                .withId(10248).forCustomer("VINET").withLine(42, "9.80", 1).build();
        repository.save(replacement);
        assertThat(repository.count()).isEqualTo(4);
        assertThat(repository.getById(OrderId.of(10248)).productIds()).containsExactly(42);
    }

    @Test
    @DisplayName("findAll hands out a copy, not the repository's own collection")
    void findAllIsImmutable() {
        assertThat(repository.findAll()).hasSize(4).isUnmodifiable();
    }

    @Test
    @DisplayName("a specification replaces a hand-written query method")
    void specificationDrivesTheQuery() {
        assertThat(repository.findAll(OrderSpecifications.placedBy(CustomerId.of("VINET"))))
                .extracting(Order::orderId)
                .containsExactly(OrderId.of(10248), OrderId.of(10249));
        assertThat(repository.findAll(OrderSpecifications.placedBy(CustomerId.of("ALFKI")))).isEmpty();
    }

    @Test
    @DisplayName("specifications combine into one query")
    void specificationsCombine() {
        assertThat(repository.findAll(
                OrderSpecifications.notCancelled()
                        .and(OrderSpecifications.placedBy(CustomerId.of("TOMSP")))))
                .extracting(Order::orderId)
                .containsExactly(OrderId.of(10251));
    }

    @Test
    @DisplayName("an empty repository answers without blowing up")
    void emptyRepository() {
        OrderRepository empty = new InMemoryOrderRepository();
        assertThat(empty.count()).isZero();
        assertThat(empty.findAll()).isEmpty();
        assertThat(empty.findAll(OrderSpecifications.notCancelled())).isEmpty();
        assertThat(empty.findById(OrderId.of(10248))).isEmpty();
    }
}
