package com.github.dgaponov99.practicum.mymarket.app.exception;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(long id) {
        super("Item %d not found".formatted(id));
    }
}
