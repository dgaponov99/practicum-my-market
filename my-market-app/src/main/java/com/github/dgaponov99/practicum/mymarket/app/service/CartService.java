package com.github.dgaponov99.practicum.mymarket.app.service;

import com.github.dgaponov99.practicum.mymarket.app.event.CartItemChangeEvent;
import com.github.dgaponov99.practicum.mymarket.app.event.DomainEventBus;
import com.github.dgaponov99.practicum.mymarket.app.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.exception.ItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.CartItemRepository;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final DomainEventBus domainEventBus;

    public Mono<Integer> countByItemId(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .map(CartItem::getCount)
                .switchIfEmpty(Mono.just(0));
    }

    public Flux<CartItem> getCartItems(long userId) {
        return cartItemRepository.findAllByUserId(userId);
    }

    public Mono<Void> incrementItem(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .switchIfEmpty(itemRepository.findById(itemId)
                        .switchIfEmpty(Mono.error(new ItemNotFoundException(itemId)))
                        .map(item -> new CartItem(userId, item.getId(), 0))
                ).map(cartItem -> {
                    cartItem.setCount(cartItem.getCount() + 1);
                    return cartItem;
                })
                .flatMap(cartItemRepository::save)
                .doOnSuccess(cartItem -> domainEventBus.publish(new CartItemChangeEvent(userId, cartItem.getItemId())))
                .then();
    }

    public Mono<Void> decrementItem(long userId, long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .switchIfEmpty(Mono.error(new CartItemNotFoundException(userId, itemId)))
                .map(cartItem -> {
                    cartItem.setCount(cartItem.getCount() - 1);
                    return cartItem;
                })
                .flatMap(cartItem -> {
                    if (cartItem.getCount() <= 0) {
                        return cartItemRepository.deleteById(itemId);
                    } else {
                        return cartItemRepository.save(cartItem).then();
                    }
                })
                .doOnSuccess(cartItem -> domainEventBus.publish(new CartItemChangeEvent(userId, itemId)));
    }

    public Mono<Void> deleteCart(long userId) {
        return cartItemRepository.deleteCartItemsByUserId(userId);
    }

}
