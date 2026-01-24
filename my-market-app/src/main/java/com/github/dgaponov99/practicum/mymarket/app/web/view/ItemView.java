package com.github.dgaponov99.practicum.mymarket.app.web.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ItemView {

    private Long id;
    private String title;
    private String description;
    private long price;
    private int count;

    @JsonIgnore
    public String getImgPath() {
        return "images/%s".formatted(id);
    }

}
