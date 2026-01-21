package com.github.dgaponov99.practicum.mymarket.percistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Table("order_item")
public class OrderItem {

    @Id
    private Long id;
    private Long orderId;
    private Long itemId;
    private int count;

    public OrderItem(long orderId, long itemId, int count) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.count = count;
    }
}
