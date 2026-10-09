package com.lectures.business.design.domain;

public final class Customer {

    // Immutable fields
    private final String customerId;
    private final String contactName;
    private final String companyName;
    private final String contactTitle;

    // Overload bir constructor eklediğimiz için default constructor ezilmiş oldu.
    public Customer(String customerId, String contactName, String companyName, String contactTitle) {
        this.customerId = requireText("customerId", customerId);
        this.companyName = requireText("companyName", companyName);
        this.contactTitle = requireText("contactTitle", contactTitle);
        this.contactName = requireText("contactName", contactName);
    }

    private static String requireText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            //System.out.println(fieldName + " can not be null or blank!");
            throw new IllegalArgumentException(fieldName + " can not be null or blank!");
        }
        return value;
    }

    public String getCustomerId() {
        return customerId;
    }
    public String getContactName() {
        return contactName;
    }
    public String getCompanyName() {
        return companyName;
    }
    public String getContactTitle() {
        return contactTitle;
    }
}
