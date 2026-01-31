package com.github.dgaponov99.practicum.mymarket.app.event;

public record CartItemChangeEvent(long userId, Long itemId) {
}
