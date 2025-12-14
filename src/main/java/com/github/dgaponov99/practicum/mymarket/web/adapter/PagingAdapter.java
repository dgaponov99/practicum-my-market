package com.github.dgaponov99.practicum.mymarket.web.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

@RequiredArgsConstructor
public class PagingAdapter {

    private final Page<?> page;

    public int getPageSize() {
        return page.getSize();
    }

    public int getPageNumber() {
        return page.getNumber();
    }

    public boolean hasPrevious() {
        return page.hasPrevious();
    }

    public boolean hasNext() {
        return page.hasNext();
    }
}
