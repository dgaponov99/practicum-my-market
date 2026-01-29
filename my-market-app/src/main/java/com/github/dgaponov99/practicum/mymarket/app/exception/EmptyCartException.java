package com.github.dgaponov99.practicum.mymarket.app.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Current cart is empty");
    }
}
