package com.github.dgaponov99.practicum.mymarket.payment.exception;

public class AccountNotFoundException extends IllegalStateException {

    public AccountNotFoundException() {
        super();
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
