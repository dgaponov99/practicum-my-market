package com.github.dgaponov99.practicum.mymarket.service;

import com.github.dgaponov99.practicum.mymarket.exception.EmptyCartException;
import com.github.dgaponov99.practicum.mymarket.exception.ItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.CartItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order create() {
        var cartItems = cartItemRepository.findAll();
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }
        var order = new Order();
        cartItems.forEach(cartItem -> {
            var item = itemRepository.findById(cartItem.getItemId()).orElseThrow(() -> new ItemNotFoundException(cartItem.getItemId()));
            order.addItem(item, cartItem.getCount());
        });
        cartItemRepository.deleteAll();
        order.setOrderDate(Instant.now());
        return orderRepository.save(order);
    }

}
