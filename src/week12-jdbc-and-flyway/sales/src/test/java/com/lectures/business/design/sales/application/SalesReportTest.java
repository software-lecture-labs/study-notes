package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.api.CustomerRevenue;
import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.domain.OrderBuilder;
import com.lectures.business.design.sales.persistence.InMemoryOrderRepository;
import com.lectures.business.design.sales.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesReportTest {

    private SalesReport report;

    @BeforeEach
    void setUp() {
        OrderRepository repository = new InMemoryOrderRepository();
        repository.save(OrderBuilder.anOrder()
                .withId(10248).forCustomer("VINET").withLine(11, "14.00", 10).shipped().build());
        repository.save(OrderBuilder.anOrder()
                .withId(10249).forCustomer("VINET").withLine(11, "10.00", 3).confirmed().build());
        repository.save(OrderBuilder.anOrder()
                .withId(10250).forCustomer("TOMSP").withLine(11, "99.00", 5).cancelled().build());
        repository.save(OrderBuilder.anOrder()
                .withId(10251).forCustomer("TOMSP").withLine(11, "20.00", 4).build());

        report = new SalesReport(repository, new OrderMapper());
    }

    @Test
    @DisplayName("cancelled orders earn nothing - the rule now has a name")
    void revenueIgnoresCancelledOrders() {
        assertThat(report.revenueByCustomer())
                .containsExactly(
                        new CustomerRevenue("TOMSP", new BigDecimal("80.00"), "TRY"),
                        new CustomerRevenue("VINET", new BigDecimal("170.00"), "TRY"));

        assertThat(report.totals().revenue()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(report.totals().orderCount()).isEqualTo(3);
        assertThat(report.totals().currency()).isEqualTo("TRY");
    }

    @Test
    @DisplayName("countByStatus reports the status name, not the enum")
    void countByStatusCountsEverything() {
        assertThat(report.countByStatus())
                .containsEntry("SHIPPED", 1L)
                .containsEntry("CONFIRMED", 1L)
                .containsEntry("CANCELLED", 1L)
                .containsEntry("DRAFT", 1L);
    }

    @Test
    @DisplayName("topCustomers ranks by revenue, highest first")
    void topCustomersAreRanked() {
        assertThat(report.topCustomers(1))
                .extracting(CustomerRevenue::customerId)
                .containsExactly("VINET");
        assertThat(report.topCustomers(5))
                .extracting(CustomerRevenue::customerId)
                .containsExactly("VINET", "TOMSP");
    }

    @Test
    @DisplayName("ordersOf goes through the mapper: the caller never sees an Order")
    void ordersOfReturnsSummaries() {
        assertThat(report.ordersOf("VINET"))
                .extracting(OrderSummary::orderId)
                .containsExactly(10248, 10249);
        assertThat(report.ordersOf("ALFKI")).isEmpty();
    }

    @Test
    @DisplayName("an unknown id is rejected by the type, not by the report")
    void anInvalidIdIsRejectedAtTheBoundary() {
        assertThatThrownBy(() -> report.ordersOf("NOBODY"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("an empty repository reports zero, not an error")
    void emptyRepositoryReportsZero() {
        SalesReport empty = new SalesReport(new InMemoryOrderRepository(), new OrderMapper());

        assertThat(empty.totals().orderCount()).isZero();
        assertThat(empty.totals().revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.revenueByCustomer()).isEmpty();
        assertThat(empty.topCustomers(3)).isEmpty();
    }

    @Test
    @DisplayName("no public method of the report mentions a domain type")
    void noDomainTypeLeaksFromTheReport() {
        for (Method method : SalesReport.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            assertThat(method.toGenericString())
                    .as(method.getName())
                    .doesNotContain("com.lectures.business.design.sales.domain");
        }
    }

    @Test
    @DisplayName("the report refuses to be built without its dependencies")
    void dependenciesAreRequired() {
        assertThatThrownBy(() -> new SalesReport(null, new OrderMapper()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SalesReport(new InMemoryOrderRepository(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
