package com.lectures.business.design;

import com.lectures.business.design.domain.Address;

public class Main {
    public static void main(String[] args) {
        var workAddress = new Address(
                "1234 Main St",
                "Washington",
                "12345",
                "USA"
        );
        var workAddressAgain = new Address(
                "1234 Main St",
                "Washington",
                "12345",
                "USA"
        );
        System.out.println("Address type equality: " + workAddress.equals(workAddressAgain)); // true
        System.out.println("Address hash code equality: " + (workAddress.hashCode() == workAddressAgain.hashCode())); // true
        System.out.println("Address reference equality: " + (workAddress == workAddressAgain)); // false
        System.out.println(workAddress); // calls toString() method

        var invalidAddress = new Address("", "Berlin", "10115", "Germany");
        // should throw an exception due to empty street
        System.out.println(invalidAddress);
    }
}
