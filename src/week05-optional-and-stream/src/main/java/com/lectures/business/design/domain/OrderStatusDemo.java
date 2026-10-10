package com.lectures.business.design.domain;

/**
 * Durum makinesini elle gozlemlemek icin kucuk bir demo.
 *
 * <p>Bir test degil: iddia icermez, yalnizca yazdirir. Bu yuzden src/main
 * altinda duruyor. Kurallarin kaniti OrderStatusTest icinde.
 */
public class OrderStatusDemo {

    public static void main(String[] args) {
        OrderStatus draft = OrderStatus.DRAFT;
        System.out.println("DRAFT allowed transitions: " + draft.allowedTransitions());
        System.out.println("Can DRAFT transition to CONFIRMED? " + draft.canTransitionTo(OrderStatus.CONFIRMED));
        System.out.println("Is DRAFT terminal? " + draft.isTerminal());
        System.out.println("Is DRAFT modifiable? " + draft.isModifiable());
    }
}
