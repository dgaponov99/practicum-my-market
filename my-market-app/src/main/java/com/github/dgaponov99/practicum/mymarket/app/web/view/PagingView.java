package com.github.dgaponov99.practicum.mymarket.app.web.view;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PagingView {

    private int pageSize;
    private int pageNumber;
    private boolean hasPrevious;
    private boolean hasNext;

}
