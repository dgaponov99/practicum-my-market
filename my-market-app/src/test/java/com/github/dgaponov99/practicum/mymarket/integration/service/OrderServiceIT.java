package com.github.dgaponov99.practicum.mymarket.integration.service;

import com.github.dgaponov99.practicum.mymarket.exception.EmptyCartException;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.OrderItem;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.CartItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.OrderItemRepository;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.OrderRepository;
import com.github.dgaponov99.practicum.mymarket.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderServiceIT extends ServiceIT {

    @Autowired
    OrderService orderService;

    @Autowired
    ItemRepository itemRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    CartItemRepository cartItemRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void findAll_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .flatMap(createdItem -> {
                    var expectOrder = new Order();
                    expectOrder.setOrderDate(LocalDateTime.now());
                    return orderRepository.save(expectOrder);
                })
                .flatMap(createdOrder -> {
                    return orderItemRepository.save(new OrderItem(createdOrder.getId(), item.getId(), 2));
                })
                .then();

        setupData.thenMany(orderService.findAll())
                .collectList()
                .doOnNext(orders -> assertThat(orders).hasSize(1))
                .block();
    }

    @Test
    void findAll_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .then();

        setupData.thenMany(orderService.findAll())
                .collectList()
                .doOnNext(orders -> assertThat(orders).isEmpty())
                .block();
    }

    @Test
    void findById_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupOrderData = itemRepository.save(item)
                .flatMap(createdItem -> {
                    var order = new Order();
                    order.setOrderDate(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .flatMap(createdOrder -> orderItemRepository.save(new OrderItem(createdOrder.getId(), item.getId(), 2))
                        .thenReturn(createdOrder));

        setupOrderData.flatMap(expectOrder -> {
            return orderService.findById(expectOrder.getId())
                    .flatMap(actualOrder -> orderService.getItems(actualOrder.getId())
                            .collectList()
                            .doOnNext(actualOrderItems -> assertAll(
                                    () -> assertEquals(expectOrder.getId(), actualOrder.getId()),
                                    () -> assertThat(actualOrder.getOrderDate()).isCloseTo(expectOrder.getOrderDate(), within(1, ChronoUnit.MICROS)),
                                    () -> assertEquals(1, actualOrderItems.size())
                            )));
        }).block();
    }

    @Test
    void findById_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupOrderData = itemRepository.save(item)
                .flatMap(createdItem -> {
                    var order = new Order();
                    order.setOrderDate(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .flatMap(createdOrder -> orderItemRepository.save(new OrderItem(createdOrder.getId(), item.getId(), 2))
                        .thenReturn(createdOrder));

        setupOrderData.flatMap(order -> orderService.findById(100500L))
                .doOnNext(actualOrder -> assertThat(actualOrder).isNull())
                .block();
    }

    @Test
    void create_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .flatMap((createdItem) -> {
                    var cartItem = new CartItem(createdItem.getId(), 2, true);
                    return cartItemRepository.save(cartItem);
                })
                .then();

        setupData.then(orderService.create())
                .flatMap(actualOrder -> orderService.getItems(actualOrder.getId())
                        .collectList()
                )
                .doOnNext(actualOrderItems -> assertAll(
                        () -> assertThat(actualOrderItems)
                                .isNotNull()
                                .hasSize(1)
                                .element(0)
                                .satisfies(firstOrderItem -> assertThat(firstOrderItem.getItemId()).isEqualTo(item.getId()))
                                .satisfies(firstOrderItem -> assertThat(firstOrderItem.getCount()).isEqualTo(2))
                ))
                .block();
    }

    @Test
    void create_emptyCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .then();

        StepVerifier.create(setupData.then(orderService.create()))
                .expectError(EmptyCartException.class)
                .verify();
    }

}
