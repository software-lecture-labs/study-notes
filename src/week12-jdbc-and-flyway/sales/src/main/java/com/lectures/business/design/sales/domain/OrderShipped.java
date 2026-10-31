package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.Address;
import java.time.LocalDate;

public record OrderShipped(OrderId orderId, CustomerId customerId, LocalDate shippedDate, Address destination)
        implements OrderEvent {

}
