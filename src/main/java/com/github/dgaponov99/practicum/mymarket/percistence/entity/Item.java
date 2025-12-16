package com.github.dgaponov99.practicum.mymarket.percistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Item {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "item_id")
    private Long id;

    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(nullable = false)
    private long price;

}
