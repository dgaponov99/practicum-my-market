package com.github.dgaponov99.practicum.mymarket.app.percistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table("cart_item")
public class CartItem implements Persistable<Long> {

    @Id
    @Column("item_id")
    private Long itemId;

    private int count;

    @Transient
    @Setter(AccessLevel.NONE)
    private boolean isNew;

    @Nullable
    @Override
    public Long getId() {
        return itemId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
