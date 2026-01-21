package com.github.dgaponov99.practicum.mymarket.integration.service;

import com.github.dgaponov99.practicum.mymarket.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.CartItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import com.github.dgaponov99.practicum.mymarket.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class CartServiceIT extends ServiceIT {

    @Autowired
    CartService cartService;

    @Autowired
    ItemRepository itemRepository;
    @Autowired
    CartItemRepository cartItemRepository;

    @Test
    void getCartItems_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .flatMap((createdItem) -> {
                    var cartItem = new CartItem(createdItem.getId(), 2, true);
                    return cartItemRepository.save(cartItem);
                })
                .then();

        setupData.thenMany(cartService.getCartItems())
                .collectList()
                .doOnNext(cartItems -> assertThat(cartItems)
                        .hasSize(1)
                        .first()
                        .satisfies(actualCartItem -> {
                            assertThat(actualCartItem.getItemId()).isEqualTo(item.getId());
                            assertThat(actualCartItem.getCount()).isEqualTo(2);
                        })
                )
                .block();
    }

    @Test
    void getCartItems_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .then();

        setupData.thenMany(cartService.getCartItems())
                .collectList()
                .doOnNext(cartItems -> assertThat(cartItems).isEmpty())
                .block();
    }

    @Test
    void countByItemId_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .then(cartItemRepository.save(new CartItem(item.getId(), 2, true)))
                .then();

        setupData.then(cartService.countByItemId(item.getId()))
                .doOnNext(countBYItemId -> assertThat(countBYItemId).isEqualTo(2))
                .block();
    }

    @Test
    void countByItemId_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .then(cartItemRepository.save(new CartItem(item.getId(), 2, true)))
                .then();

        setupData.then(cartService.countByItemId(100500L))
                .doOnNext(countByItemId -> assertThat(countByItemId).isEqualTo(0))
                .block();
    }

    @Test
    void incrementItem_noItemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .then();

        setupData.then(cartService.incrementItem(item.getId()))
                .then(cartItemRepository.findById(item.getId()))
                .doOnNext(cartItem -> assertThat(cartItem)
                        .isNotNull()
                        .extracting(CartItem::getCount).isEqualTo(1))
                .block();
    }

    @Test
    void incrementItem_itemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .flatMap(createdItem -> cartItemRepository.save(new CartItem(createdItem.getId(), 2, true)))
                .then();

        setupData.then(cartService.incrementItem(item.getId()))
                .then(cartItemRepository.findById(item.getId()))
                .doOnNext(cartItem -> assertThat(cartItem)
                        .isNotNull()
                        .extracting(CartItem::getCount).isEqualTo(3))
                .block();
    }

    @Test
    void decrementItem_itemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .flatMap(createdItem -> cartItemRepository.save(new CartItem(createdItem.getId(), 2, true)))
                .then();

        setupData.then(cartService.decrementItem(item.getId()))
                .then(cartItemRepository.findById(item.getId()))
                .doOnNext(cartItem -> assertThat(cartItem)
                        .isNotNull()
                        .extracting(CartItem::getCount).isEqualTo(1))
                .block();
    }

    @Test
    void decrementItem_lastItemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .flatMap(createdItem -> cartItemRepository.save(new CartItem(createdItem.getId(), 1, true)))
                .then();

        setupData.then(cartService.decrementItem(item.getId()))
                .then(cartItemRepository.findById(item.getId()))
                .doOnNext(cartItem -> assertThat(cartItem).isNull())
                .block();
    }

    @Test
    void decrementItem_noItemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .flatMap(createdItem -> cartItemRepository.save(new CartItem(createdItem.getId(), 1, true)))
                .then();

        StepVerifier.create(setupData.then(cartService.decrementItem(100500L)))
                .expectError(CartItemNotFoundException.class)
                .verify();
    }

}
