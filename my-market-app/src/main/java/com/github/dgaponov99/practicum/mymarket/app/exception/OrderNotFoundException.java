package com.github.dgaponov99.practicum.mymarket.app.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(long id) {
        super("Order %d not found".formatted(id));
    }
}
