package com.github.dgaponov99.practicum.mymarket.percistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CartItem {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false)
    private int count;

}
