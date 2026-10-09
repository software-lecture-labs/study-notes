package com.lectures.business.design;

import com.lectures.business.design.domain.AnemicOrder;
import com.lectures.business.design.domain.AnemicOrderLine;
import com.lectures.business.design.domain.Money;
import com.lectures.business.design.domain.Order;
import com.lectures.business.design.domain.OrderCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;

public class BusinessObjects {

    public static void main(String[] args) {
        Order rich = new Order(10248, "VINET", LocalDate.of(1996, 7, 4));
        AnemicOrder anemic = new AnemicOrder();
        for (int i = 0; i < PRODUCTS.length; i++) {
            rich.addLine(PRODUCTS[i], Money.tl(PRICES[i]), QUANTITIES[i], new BigDecimal(DISCOUNTS[i]));
            AnemicOrderLine line = new AnemicOrderLine();
            line.setProductId(PRODUCTS[i]);
            line.setUnitPrice(Double.parseDouble(PRICES[i]));
            line.setQuantity(QUANTITIES[i]);
            line.setDiscount(Double.parseDouble(DISCOUNTS[i]));
            anemic.getLines().add(line);
        }
        System.out.println("anemic total : " + new OrderCalculator().total(anemic));
        System.out.println("rich   total : " + rich.total());
    }
    private static final int[] PRODUCTS = {11, 42, 72, 28, 39};
    private static final String[] PRICES = {"14.00", "9.80", "34.80", "45.60", "18.00"};
    private static final int[] QUANTITIES = {12, 10, 5, 9, 21};
    private static final String[] DISCOUNTS = {"0.05", "0.15", "0.10", "0.25", "0.05"};
}
