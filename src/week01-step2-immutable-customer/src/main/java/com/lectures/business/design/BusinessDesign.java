package com.lectures.business.design;

import com.lectures.business.design.domain.Customer;

public class BusinessDesign {

    public static void main(String[] args) {
        try {
            Customer bob = new Customer(
                    "BOBIB",
                    "Babi bobi",
                    "AZON CORP",
                    "Owner"
            );

//            Customer bobRight = new Customer(
//                    bob.getCustomerId(),
//                    bob.getContactName(),
//                    "Azon Crop Ltd",
//                    bob.getCompanyName()
//            );

        System.out.println(bob.getContactName()
                    + " (" + bob.getContactTitle() + ")");
        } catch (IllegalArgumentException e) {
            // loglama yapılır
            //System.out.println(e);
        }
    }
}
