package com.lectures.business.design.domain;

public class AnemicOrderLine {

    private int productId;
    private double unitPrice;
    private int quantity;
    private double discount;

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
}
