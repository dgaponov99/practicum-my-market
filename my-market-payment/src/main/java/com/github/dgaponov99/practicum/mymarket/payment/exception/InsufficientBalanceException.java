package com.github.dgaponov99.practicum.mymarket.payment.exception;

public class InsufficientBalanceException extends IllegalStateException {

    public InsufficientBalanceException() {
        super();
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
