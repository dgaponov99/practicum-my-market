package com.github.dgaponov99.practicum.mymarket.percistence.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class OrderItem {

    @EmbeddedId
    private OrderItemPK pk;

    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    @MapsId("itemId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", referencedColumnName = "item_id")
    private Item item;

    @Column(nullable = false)
    private int count;

    protected OrderItem() {
    }

    public OrderItem(Order order, Item item, int count) {
        this.order = order;
        this.item = item;
        this.count = count;
        this.pk = new OrderItemPK(order.getId(), item.getId());
    }

}
