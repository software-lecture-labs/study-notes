package com.lectures.business.design;

import java.math.BigDecimal;

import com.lectures.business.design.domain.Address;
import com.lectures.business.design.domain.CustomerId;
import com.lectures.business.design.domain.Money;
import com.lectures.business.design.domain.Order;
import com.lectures.business.design.domain.OrderBook;
import com.lectures.business.design.domain.OrderId;
import com.lectures.business.design.domain.OrderStatus;

public class BusinessDesign {

    public static void main(String[] args) {
        OrderBook sampleOrders = createSampleOrders();

        // findById sample
        var order = sampleOrders.findById(new OrderId(10248));
        order.ifPresent(o -> System.out.println(o.orderId() + " " + o.customerId() + " " + o.status()));

        // findByCustomer sample
        var customerOrders = sampleOrders.findByCustomer(new CustomerId("HANAR"));
        System.out.println("\nHANAR's orders----");
        customerOrders.forEach(o -> {
            System.out.println(o.orderId() + " " + o.customerId() + " " + o.status() + " " + o.total());
            o.lines().forEach(line -> System.out.println("  " + line));
        });

        // findByStatus sample
        var statusOrders = sampleOrders.findByStatus(OrderStatus.CONFIRMED);
        System.out.println("\nConfirmed orders----");
        statusOrders.forEach(o -> System.out.println(o.orderId() + " " + o.customerId() + " " + o.status() + " " + o.total()));

        // topCustomer sample
        var topCustomerOrders = sampleOrders.topCustomers(3);
        System.out.println("\nTop 3 customers----");
        topCustomerOrders.forEach(o -> System.out.println(o));

        // total revenue sample
        var totalRevenue = sampleOrders.totalRevenue();
        System.out.println("\nTotal revenue----");
        System.out.println(totalRevenue);

        var anotherOrder = new Order(OrderId.of(10248), CustomerId.of("VINET"), java.time.LocalDate.of(1996, 7, 4));
        System.out.println(anotherOrder);

    }

    static OrderBook createSampleOrders() {
        OrderBook orderBook = new OrderBook();
        var order = new Order(new OrderId(10248), new CustomerId("VINET"), java.time.LocalDate.of(1996, 7, 4));
        order.addLine(10001, Money.tl("9.40"), 10, BigDecimal.valueOf(0.1));
        order.addLine(10002, Money.tl("19.50"), 5, BigDecimal.valueOf(0.2));
        order.addLine(10003, Money.tl("5.00"), 20, BigDecimal.ZERO);
        orderBook.add(order);
        order.shipTo(new Address("Lisbon Main", "Lisbon", "1000-001", "Portugal"));
        order.confirm();

        order = new Order(new OrderId(10249), new CustomerId("TOMSP"), java.time.LocalDate.of(1996, 7, 5));
        order.addLine(10004, Money.tl("15.00"), 10, BigDecimal.valueOf(0.15));
        order.addLine(10005, Money.tl("25.00"), 5, BigDecimal.valueOf(0.1));
        orderBook.add(order);

        order = new Order(new OrderId(10250), new CustomerId("HANAR"), java.time.LocalDate.of(1996, 7, 8));
        order.addLine(10006, Money.tl("30.00"), 15, BigDecimal.valueOf(0.05));
        order.addLine(10007, Money.tl("12.00"), 8, BigDecimal.valueOf(0.1));
        orderBook.add(order);

        order = new Order(new OrderId(10251), new CustomerId("VICTE"), java.time.LocalDate.of(1996, 7, 9));
        order.addLine(10008, Money.tl("20.00"), 10, BigDecimal.ZERO);
        order.addLine(10009, Money.tl("10.00"), 5, BigDecimal.ZERO);
        orderBook.add(order);

        order = new Order(new OrderId(10252), new CustomerId("SUPRD"), java.time.LocalDate.of(1996, 7, 10));
        order.addLine(10010, Money.tl("50.00"), 5, BigDecimal.valueOf(0.2));
        order.addLine(10011, Money.tl("30.00"), 3, BigDecimal.valueOf(0.1));
        orderBook.add(order);
        order.cancel();

        order = new Order(new OrderId(10253), new CustomerId("CHOPS"), java.time.LocalDate.of(1996, 7, 11));
        order.addLine(10012, Money.tl("40.00"), 7, BigDecimal.valueOf(0.05));
        order.addLine(10013, Money.tl("22.00"), 4, BigDecimal.valueOf(0.1));
        orderBook.add(order);

        return orderBook;
    }
}
