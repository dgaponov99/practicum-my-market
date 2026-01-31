package com.github.dgaponov99.practicum.mymarket.app.web.view;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class OrderPageView {

    private long id;
    private long userId;
    private List<OrderItemView> items;
    private long totalPrice;

}
