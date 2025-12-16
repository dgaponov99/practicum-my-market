package com.github.dgaponov99.practicum.mymarket.web.view;

import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
public class ItemView {

    private Long id;
    private String title;
    private String description;
    private long price;
    private int count;

    public Long id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public long price() {
        return price;
    }

    public int count() {
        return count;
    }

    public String imgPath() {
        return "images/%s".formatted(id);
    }

}
