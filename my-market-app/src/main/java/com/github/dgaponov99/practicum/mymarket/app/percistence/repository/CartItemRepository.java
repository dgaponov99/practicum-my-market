package com.github.dgaponov99.practicum.mymarket.app.percistence.repository;

import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.CartItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Mono<Void> deleteCartItemsByUserId(long userId);

    Mono<CartItem> findByUserIdAndItemId(long userId, long itemId);

    Flux<CartItem> findAllByUserId(long userId);
}
