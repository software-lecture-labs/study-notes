
import com.lectures.business.design.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class OrderBuilder {

    private static final Address DEFAULT_DESTINATION = new Address("59 rue de l'Abbaye", "Reims", "51100", "France");
    private static final LocalDate DEFAULT_DATE = LocalDate.of(1996, 7, 4);

    private record LineSpec(int productId, Money unitPrice, int quantity, BigDecimal discount) {

    }
    private OrderId orderId = OrderId.of(10248);
    private CustomerId customerId = CustomerId.of("VINET");
    private LocalDate orderDate = DEFAULT_DATE;
    private Address destination = DEFAULT_DESTINATION;
    private OrderStatus target = OrderStatus.DRAFT;
    private final List<LineSpec> lines = new ArrayList<>();

    private OrderBuilder() {

    }

    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public OrderBuilder withId(int value) {
        orderId = OrderId.of(value);
        return this;
    }

    public OrderBuilder forCustomer(String value) {
        customerId = CustomerId.of(value);
        return this;
    }

    public OrderBuilder orderedOn(LocalDate date) {
        orderDate = date;
        return this;
    }

    public OrderBuilder shippingTo(Address address) {
        destination = address;
        return this;
    }

    public OrderBuilder withLine(int productId, String unitPrice, int quantity) {
        lines.add(new LineSpec(productId, Money.tl(unitPrice), quantity, BigDecimal.ZERO));
        return this;
    }

    public OrderBuilder withDiscountedLine(int productId, String unitPrice, int quantity, String discount) {
        lines.add(new LineSpec(productId, Money.tl(unitPrice), quantity, new BigDecimal(discount)));
        return this;
    }

    public OrderBuilder confirmed() {
        target = OrderStatus.CONFIRMED;
        return this;
    }

    public OrderBuilder shipped() {
        target = OrderStatus.SHIPPED;
        return this;
    }

    public OrderBuilder cancelled() {
        target = OrderStatus.CANCELLED;
        return this;
    }

    public Order build() {
        Order order = new Order(orderId, customerId, orderDate);
        order.shipTo(destination);
        for (LineSpec line : lines) {
            order.addLine(line.productId(), line.unitPrice(), line.quantity(), line.discount());
        }
        return advance(order);
    }

    private Order advance(Order order) {
        switch (target) {
            case DRAFT -> {
            }
            case CANCELLED ->
                order.cancel();
            case CONFIRMED ->
                order.confirm();
            case SHIPPED -> {
                order.confirm();
                order.ship(orderDate.plusDays(12));
            }
        }
        return order;
    }
}
