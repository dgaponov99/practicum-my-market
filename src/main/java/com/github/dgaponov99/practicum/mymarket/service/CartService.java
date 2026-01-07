package com.github.dgaponov99.practicum.mymarket.service;

import com.github.dgaponov99.practicum.mymarket.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.exception.ItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.CartItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public int countByItemId(long itemId) {
        return cartItemRepository.findById(itemId).map(CartItem::getCount).orElse(0);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems() {
        return cartItemRepository.findAll();
    }

    @Transactional
    public void incrementItem(long itemId) {
        var cartItem = cartItemRepository.findById(itemId).orElseGet(() -> {
            itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
            var newCartItem = new CartItem();
            newCartItem.setItemId(itemId);
            return newCartItem;
        });
        cartItem.setCount(cartItem.getCount() + 1);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void decrementItem(long itemId) {
        var cartItem = cartItemRepository.findById(itemId).orElseThrow(() -> new CartItemNotFoundException(itemId));
        cartItem.setCount(cartItem.getCount() - 1);
        if (cartItem.getCount() <= 0) {
            cartItemRepository.deleteById(itemId);
        } else {
            cartItemRepository.save(cartItem);
        }
    }

}
