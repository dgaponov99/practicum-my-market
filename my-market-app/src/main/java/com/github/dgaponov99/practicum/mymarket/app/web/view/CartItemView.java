package com.github.dgaponov99.practicum.mymarket.app.web.view;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class CartItemView {

    private long userId;
    private ItemView item;
    private int count;
    private long totalPrice;

}
