package com.github.dgaponov99.practicum.mymarket.app.percistence.repository;

import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}
