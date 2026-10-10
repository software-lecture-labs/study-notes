package com.lectures.business.design;

import java.math.BigDecimal;
import java.time.Clock;

import com.lectures.business.design.domain.Address;
import com.lectures.business.design.domain.Customer;
import com.lectures.business.design.domain.CustomerId;
import com.lectures.business.design.domain.Money;
import com.lectures.business.design.domain.Order;
import com.lectures.business.design.domain.OrderFactory;
import com.lectures.business.design.domain.SequentialOrderIdGenerator;

public class BusinessDesign {

    public static void main(String[] args) {
        OrderFactory orderFactory = new OrderFactory(new SequentialOrderIdGenerator(1), Clock.systemDefaultZone());
        Customer customer = new Customer(new CustomerId("ALFKI"), "Contoso Inc", new Address("Main Contoso St", "45001", "New York", "USA"));
        Order order = orderFactory.draftFor(customer);
        order.addLine(10001, Money.tl("9.40"), 10, BigDecimal.valueOf(0.1));
        order.addLine(10002, Money.tl("19.50"), 5, BigDecimal.valueOf(0.2));
        order.addLine(10003, Money.tl("5.00"), 20, BigDecimal.ZERO);

        System.out.println(order.total());
    }
}
