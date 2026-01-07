package com.github.dgaponov99.practicum.mymarket.integration.service;

import com.github.dgaponov99.practicum.mymarket.exception.EmptyCartException;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.CartItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.OrderRepository;
import com.github.dgaponov99.practicum.mymarket.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceIT extends ServiceIT {

    @Autowired
    OrderService orderService;

    @Autowired
    ItemRepository itemRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    CartItemRepository cartItemRepository;

    @Test
    void findAll_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var expectOrder = new Order();
        expectOrder.setOrderDate(Instant.now());
        expectOrder.addItem(item, 2);
        var orderId = orderRepository.saveAndFlush(expectOrder).getId();

        var actualOrders = orderService.findAll();
        assertEquals(1, actualOrders.size());
        assertEquals(expectOrder, actualOrders.get(0));
    }

    @Test
    void findAll_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var actualOrders = orderService.findAll();
        assertEquals(0, actualOrders.size());
    }

    @Test
    void findById_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var expectOrder = new Order();
        expectOrder.setOrderDate(Instant.now());
        expectOrder.addItem(item, 2);
        var orderId = orderRepository.saveAndFlush(expectOrder).getId();

        var actualOrder = orderService.findById(orderId).orElseThrow();

        assertAll(() -> {
            assertEquals(expectOrder, actualOrder);
            assertEquals(orderId, actualOrder.getId());
            assertEquals(expectOrder.getOrderDate(), actualOrder.getOrderDate());
            assertEquals(expectOrder.getOrderItems(), actualOrder.getOrderItems());
        });
    }

    @Test
    void findById_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var order = new Order();
        order.setOrderDate(Instant.now());
        order.addItem(item, 2);
        orderRepository.saveAndFlush(order).getId();

        var actualOrderOpt = orderService.findById(100500L);

        assertTrue(actualOrderOpt.isEmpty());
    }

    @Test
    void create_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        var cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(2);
        cartItem = cartItemRepository.saveAndFlush(cartItem);

        var actualOrder = orderService.create();

        assertNotNull(actualOrder);
        final var finalItem = item;
        assertAll(() -> {
            assertNotNull(actualOrder.getId());
            assertNotNull(actualOrder.getOrderDate());
            assertEquals(1, actualOrder.getOrderItems().size());
            assertEquals(finalItem, actualOrder.getOrderItems().get(0).getItem());
            assertEquals(2, actualOrder.getOrderItems().get(0).getCount());
        });
    }

    @Test
    void create_emptyCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000);
        item = itemRepository.saveAndFlush(item);

        assertThrows(EmptyCartException.class, () -> orderService.create());
    }

}
