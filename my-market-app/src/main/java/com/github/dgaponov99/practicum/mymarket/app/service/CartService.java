package com.github.dgaponov99.practicum.mymarket.app.service;

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

    public Mono<Integer> countByItemId(long itemId) {
        return cartItemRepository.findById(itemId)
                .map(CartItem::getCount)
                .switchIfEmpty(Mono.just(0));
    }

    public Flux<CartItem> getCartItems() {
        return cartItemRepository.findAll();
    }

    public Mono<Void> incrementItem(long itemId) {
        return cartItemRepository.findById(itemId)
                .switchIfEmpty(itemRepository.findById(itemId)
                        .switchIfEmpty(Mono.error(new ItemNotFoundException(itemId)))
                        .map(item -> new CartItem(item.getId(), 0, true))
                ).map(cartItem -> {
                    cartItem.setCount(cartItem.getCount() + 1);
                    return cartItem;
                })
                .flatMap(cartItemRepository::save)
                .then();
    }

    public Mono<Void> decrementItem(long itemId) {
        return cartItemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new CartItemNotFoundException(itemId)))
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
                });
    }

}
