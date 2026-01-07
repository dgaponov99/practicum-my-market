package com.github.dgaponov99.practicum.mymarket.percistence.repository;

import com.github.dgaponov99.practicum.mymarket.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
