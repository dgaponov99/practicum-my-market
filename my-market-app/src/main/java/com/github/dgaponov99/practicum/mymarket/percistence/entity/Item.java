package com.github.dgaponov99.practicum.mymarket.percistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Item implements Persistable<Long> {

    @Id
    @Column("item_id")
    private Long id;

    private String title;
    private String description;
    private long price;

    @Transient
    @Setter(AccessLevel.NONE)
    private boolean isNew;

    @Override
    public boolean isNew() {
        return isNew;
    }
}
