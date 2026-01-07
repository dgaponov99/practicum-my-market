package com.github.dgaponov99.practicum.mymarket.percistence.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class OrderItemPK {

    Long orderId;
    Long itemId;

}
