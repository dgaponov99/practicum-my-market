package com.github.dgaponov99.practicum.mymarket.app.web.view;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class CartPageView {

    private long userId;
    private List<CartItemView> cartItems;
    private long totalPrice;

}
