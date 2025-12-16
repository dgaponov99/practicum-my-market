package com.github.dgaponov99.practicum.mymarket.percistence.repository;

import com.github.dgaponov99.practicum.mymarket.percistence.entity.OrderItem;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.OrderItemPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemPK> {

    List<OrderItem> findByOrder_Id(Long orderId);
}
