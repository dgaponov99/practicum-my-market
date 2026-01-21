package com.github.dgaponov99.practicum.mymarket.exception;

public class ImageItemNotFoundException extends RuntimeException {

    public ImageItemNotFoundException(long itemId) {
        super("Image fot item %s not found".formatted(itemId));
    }
}
