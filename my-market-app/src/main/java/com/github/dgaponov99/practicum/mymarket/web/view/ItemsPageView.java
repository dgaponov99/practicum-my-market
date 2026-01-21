package com.github.dgaponov99.practicum.mymarket.web.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemsPageView {

    private List<ItemView> items;
    private PagingView paging;

}
