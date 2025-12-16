package com.github.dgaponov99.practicum.mymarket.integration.service;

import com.github.dgaponov99.practicum.mymarket.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.CartItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import com.github.dgaponov99.practicum.mymarket.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class CartServiceIT extends ServiceIT {

    @Autowired
    CartService cartService;

    @Autowired
    ItemRepository itemRepository;
    @Autowired
    CartItemRepository cartItemRepository;

    @Test
    void getCartItems_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(2);
        cartItem = cartItemRepository.saveAndFlush(cartItem);

        var cartItems = cartService.getCartItems();

        assertEquals(1, cartItems.size());
        assertEquals(item.getId(), cartItems.get(0).getItemId());
        assertEquals(2, cartItems.get(0).getCount());
    }

    @Test
    void getCartItems_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItems = cartService.getCartItems();

        assertEquals(0, cartItems.size());
    }

    @Test
    void countByItemId_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(2);
        cartItem = cartItemRepository.saveAndFlush(cartItem);

        var itemCartCount = cartService.countByItemId(item.getId());

        assertEquals(2, itemCartCount);
    }

    @Test
    void countByItemId_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(2);
        cartItem = cartItemRepository.saveAndFlush(cartItem);

        var itemCartCount = cartService.countByItemId(100500L);

        assertEquals(0, itemCartCount);
    }

    @Test
    void incrementItem_noItemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        cartService.incrementItem(item.getId());

        var cartItem = cartItemRepository.findById(item.getId()).orElseThrow();

        assertEquals(1, cartItem.getCount());
    }

    @Test
    void incrementItem_itemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(2);
        cartItem = cartItemRepository.saveAndFlush(cartItem);

        cartService.incrementItem(item.getId());

        var actualCartItem = cartItemRepository.findById(item.getId()).orElseThrow();

        assertEquals(3, actualCartItem.getCount());
    }

    @Test
    void decrementItem_itemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(2);
        cartItem = cartItemRepository.saveAndFlush(cartItem);

        cartService.decrementItem(item.getId());

        var actualCartItem = cartItemRepository.findById(item.getId()).orElseThrow();

        assertEquals(1, actualCartItem.getCount());
    }

    @Test
    void decrementItem_lastItemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(1);
        cartItem = cartItemRepository.saveAndFlush(cartItem);

        cartService.decrementItem(item.getId());

        var actualCartItemOpt = cartItemRepository.findById(item.getId());
        assertTrue(actualCartItemOpt.isEmpty());
    }

    @Test
    void decrementItem_noItemInCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(1);
        cartItem = cartItemRepository.saveAndFlush(cartItem);

        assertThrows(CartItemNotFoundException.class, () -> cartService.decrementItem(100500L));
    }

}
