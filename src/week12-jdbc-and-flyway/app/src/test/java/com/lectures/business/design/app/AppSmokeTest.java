package com.lectures.business.design.app;

import com.lectures.business.design.sales.api.CustomerRevenue;
import com.lectures.business.design.sales.api.OrderLineRequest;
import com.lectures.business.design.sales.api.PlaceOrderCommand;
import com.lectures.business.design.sales.application.OrderService;
import com.lectures.business.design.sales.application.SteppingClock;
import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.shared.kernel.Address;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AppSmokeTest {

    @Test
    @DisplayName("the composition root wires one graph: what the service writes, the report reads")
    void theWiredGraphWorksEndToEnd() {
        SteppingClock clock = new SteppingClock("1996-07-04T09:00:00Z");
        List<String> sent = new ArrayList<>();
        SalesModule module = SalesModule.inMemory(clock, 10248, (customerId, message) -> sent.add(customerId + ": " + message));
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
    }
}
