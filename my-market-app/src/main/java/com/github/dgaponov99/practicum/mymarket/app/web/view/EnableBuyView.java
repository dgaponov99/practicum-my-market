package com.github.dgaponov99.practicum.mymarket.app.web.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnableBuyView {

    private boolean enabled;
    private String message;

}
