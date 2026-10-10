
import com.lectures.business.design.domain.Address;
import com.lectures.business.design.domain.Customer;
import com.lectures.business.design.domain.CustomerId;
import com.lectures.business.design.domain.Order;
import com.lectures.business.design.domain.OrderFactory;
import com.lectures.business.design.domain.OrderId;
import com.lectures.business.design.domain.OrderStatus;
import com.lectures.business.design.domain.SequentialOrderIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderFactoryTest {

    private static final Address REIMS
            = new Address("59 rue de l'Abbaye", "Reims", "51100", "France");
    private static final Address BERLIN
            = new Address("Obere Str. 57", "Berlin", "12209", "Germany");

    /** Frozen on 4 July 1996, so the test does not depend on today. */
    private static final Clock FIXED_CLOCK
            = Clock.fixed(Instant.parse("1996-07-04T10:15:30Z"), ZoneId.of("UTC"));

    private Customer customer;
    private OrderFactory factory;

    @BeforeEach
    void setUp() {
        customer = new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier", REIMS);
        factory = new OrderFactory(new SequentialOrderIdGenerator(10248), FIXED_CLOCK);
    }

    @Test
    @DisplayName("each draft gets the next order number")
    void idsAreSequential() {
        assertThat(factory.draftFor(customer).orderId()).isEqualTo(OrderId.of(10248));
        assertThat(factory.draftFor(customer).orderId()).isEqualTo(OrderId.of(10249));
        assertThat(factory.draftFor(customer).orderId()).isEqualTo(OrderId.of(10250));
    }

    @Test
    @DisplayName("the order date comes from the clock, not from LocalDate.now()")
    void dateComesFromTheClock() {
        assertThat(factory.draftFor(customer).orderDate()).isEqualTo(LocalDate.of(1996, 7, 4));
    }

    @Test
    @DisplayName("the destination defaults to the customer's own address")
    void destinationDefaultsToCustomerAddress() {
        Order order = factory.draftFor(customer);

        assertThat(order.shippingAddress()).contains(REIMS);
        assertThat(order.customerId()).isEqualTo(CustomerId.of("VINET"));
        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
    }

    @Test
    @DisplayName("a different destination can be given explicitly")
    void destinationCanBeOverridden() {
        assertThat(factory.draftFor(customer, BERLIN).shippingAddress()).contains(BERLIN);
    }

    @Test
    @DisplayName("a draft comes out empty: the factory adds no lines")
    void draftHasNoLines() {
        assertThat(factory.draftFor(customer).lines()).isEmpty();
    }

    @Test
    @DisplayName("missing dependencies and missing arguments are rejected")
    void nullsAreRejected() {
        assertThatThrownBy(() -> new OrderFactory(null, FIXED_CLOCK))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderFactory(new SequentialOrderIdGenerator(1), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> factory.draftFor(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> factory.draftFor(customer, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("the id generator refuses a non-positive starting point")
    void generatorNeedsAPositiveStart() {
        assertThatThrownBy(() -> new SequentialOrderIdGenerator(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
