package com.github.dgaponov99.practicum.mymarket.app.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(long id) {
        super("Item %d not found in cart".formatted(id));
    }
}
