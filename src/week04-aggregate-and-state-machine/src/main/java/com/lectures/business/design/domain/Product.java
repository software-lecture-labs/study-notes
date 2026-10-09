package com.lectures.business.design.domain;

public class Product {

    private int productId;              // schema: smallint NOT NULL
    private String productName;         // schema: varchar(40) NOT NULL
    private Integer categoryId;         // schema: smallint, nullable
    private String quantityPerUnit;     // schema: varchar(20)
    private float unitPrice;            // schema: real
    private short unitsInStock;         // schema: smallint
    private boolean discontinued;       // schema: integer NOT NULL (0/1)

    public Product() {
        // JavaBean contract: public no-arg constructor
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getQuantityPerUnit() {
        return quantityPerUnit;
    }

    public void setQuantityPerUnit(String quantityPerUnit) {
        this.quantityPerUnit = quantityPerUnit;
    }

    public float getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(float unitPrice) {
        this.unitPrice = unitPrice;
    }

    public short getUnitsInStock() {
        return unitsInStock;
    }

    public void setUnitsInStock(short unitsInStock) {
        this.unitsInStock = unitsInStock;
    }

    public boolean isDiscontinued() {
        return discontinued;
    }

    public void setDiscontinued(boolean discontinued) {
        this.discontinued = discontinued;
    }
}
