package com.lectures.business.design.app;

import com.lectures.business.design.sales.api.CustomerRevenue;
import com.lectures.business.design.sales.api.OrderLineRequest;
import com.lectures.business.design.sales.api.PlaceOrderCommand;
import com.lectures.business.design.sales.application.CustomerNotifier;
import com.lectures.business.design.sales.application.OrderService;
import com.lectures.business.design.sales.application.SteppingClock;
import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.shared.kernel.Address;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;

class AppSmokeTest {

    static Stream<Arguments> wirings() {
        return Stream.of(
                Arguments.of("in-memory", (BiFunction<Clock, CustomerNotifier, SalesModule>) (clock, notifier)
                        -> SalesModule.inMemory(clock, 10248, notifier)),
                Arguments.of("jdbc-h2", (BiFunction<Clock, CustomerNotifier, SalesModule>) (clock, notifier)
                        -> SalesModule.jdbc(Database.migrated(Database.h2InMemory()), clock, notifier)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wirings")
    @DisplayName("one scenario, two wirings: the use cases cannot tell memory from a database")
    void theWiredGraphWorksEndToEnd(String name, BiFunction<Clock, CustomerNotifier, SalesModule> wiring) {
        SteppingClock clock = new SteppingClock("1996-07-04T09:00:00Z");
        List<String> sent = new ArrayList<>();
        SalesModule module = wiring.apply(clock, (customerId, message) -> sent.add(customerId + ": " + message));
        module.customers().save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));

        OrderService service = module.orderService();
        int shipped = service.placeOrder(new PlaceOrderCommand("VINET",
                List.of(OrderLineRequest.of(11, "14.00", 10)))).orderId();
        int cancelled = service.placeOrder(new PlaceOrderCommand("VINET",
                List.of(OrderLineRequest.of(11, "99.00", 5)))).orderId();

        service.confirm(shipped);
        clock.advance(Duration.ofDays(3));
        service.ship(shipped);
        service.cancel(cancelled);

        assertThat(module.salesReport().revenueByCustomer())
                .containsExactly(new CustomerRevenue("VINET", new BigDecimal("140.00"), "TRY"));
        assertThat(module.salesReport().countByStatus())
                .containsEntry("SHIPPED", 1L)
                .containsEntry("CANCELLED", 1L);
        assertThat(sent).containsExactly("VINET: Order 10248 shipped on 1996-07-07 to Reims");
        assertThat(module.activityLog().entries()).hasSize(3);
    }
}
