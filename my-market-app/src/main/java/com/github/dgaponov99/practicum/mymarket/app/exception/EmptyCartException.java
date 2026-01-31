package com.github.dgaponov99.practicum.mymarket.app.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException(long userId) {
        super("Cart of user %d is empty".formatted(userId));
    }
}
