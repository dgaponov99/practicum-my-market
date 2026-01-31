package com.github.dgaponov99.practicum.mymarket.app.percistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Table("cart_item")
public class CartItem {

    @Id
    private Long id;

    @Column("user_id")
    private long userId;
    @Column("item_id")
    private long itemId;

    private int count;

    public  CartItem(long userId, long itemId, int count) {
        this.userId = userId;
        this.itemId = itemId;
        this.count = count;
    }
}
