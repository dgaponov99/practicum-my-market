package com.github.dgaponov99.practicum.mymarket.web.view;

import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
public class PagingView {

    private int pageSize;
    private int pageNumber;
    private boolean hasPrevious;
    private boolean hasNext;

    public int pageSize() {
        return pageSize;
    }

    public int pageNumber() {
        return pageNumber;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }

    public boolean hasNext() {
        return hasNext;
    }

}
