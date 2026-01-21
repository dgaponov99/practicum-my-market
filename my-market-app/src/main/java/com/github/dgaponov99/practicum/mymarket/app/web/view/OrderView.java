package com.github.dgaponov99.practicum.mymarket.app.web.view;

import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@ToString
public class OrderView {

    private long id;
    private List<ItemView> items;
    private long totalSum;

    public long id() {
        return id;
    }

    public List<ItemView> items() {
        return items;
    }

    public long totalSum() {
        return totalSum;
    }

}
