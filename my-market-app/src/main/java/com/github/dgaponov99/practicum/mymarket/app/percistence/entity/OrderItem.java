package com.github.dgaponov99.practicum.mymarket.app.percistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Table("order_item")
public class OrderItem {

    @Id
    private Long id;
    @Column("order_id")
    private long orderId;
    @Column("item_id")
    private long itemId;
    private int count;
    @Column("unit_price")
    private long unitPrice;

    public OrderItem(long orderId, long itemId, int count, long unitPrice) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.count = count;
        this.unitPrice = unitPrice;
    }
}
