package com.lectures.business.design.app.anemic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Anemic counterpart of {@code Order}. Every field is reachable through a
 * setter and {@link #getLines()} hands out the internal list, so any caller can
 * put the order into any state.
 */
public class AnemicOrder {

    private int orderId;
    private String customerId;
    private LocalDate orderDate;
    private List<AnemicOrderLine> lines = new ArrayList<>();
    private String status;
    private LocalDate shippedDate;

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public List<AnemicOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<AnemicOrderLine> lines) {
        this.lines = lines;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getShippedDate() {
        return shippedDate;
    }

    public void setShippedDate(LocalDate shippedDate) {
        this.shippedDate = shippedDate;
    }
}
