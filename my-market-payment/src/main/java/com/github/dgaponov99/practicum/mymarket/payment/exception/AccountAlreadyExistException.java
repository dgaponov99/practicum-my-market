package com.github.dgaponov99.practicum.mymarket.payment.exception;

public class AccountAlreadyExistException extends IllegalStateException {

    public AccountAlreadyExistException() {
        super();
    }

    public AccountAlreadyExistException(String message) {
        super(message);
    }
}
