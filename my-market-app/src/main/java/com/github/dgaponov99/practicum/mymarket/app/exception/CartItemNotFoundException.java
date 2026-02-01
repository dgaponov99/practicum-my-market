package com.github.dgaponov99.practicum.mymarket.app.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(long userId, long itemId) {
        super("Item %d not found in cart of user %d".formatted(itemId, userId));
    }
}
