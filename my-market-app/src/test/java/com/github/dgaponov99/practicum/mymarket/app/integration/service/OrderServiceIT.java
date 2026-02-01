package com.github.dgaponov99.practicum.mymarket.app.integration.service;

import com.github.dgaponov99.practicum.mymarket.app.exception.EmptyCartException;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.*;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.*;
import com.github.dgaponov99.practicum.mymarket.app.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
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
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.findById(1L).switchIfEmpty(userRepository.save(new User(null, "user", "password", 1L, false))).block();
    }

    @Test
    void findByUserId_success() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .flatMap(createdItem -> {
                    var expectOrder = new Order();
                    expectOrder.setUserId(1L);
                    expectOrder.setOrderDate(LocalDateTime.now());
                    return orderRepository.save(expectOrder);
                })
                .flatMap(createdOrder ->
                        orderItemRepository.save(new OrderItem(createdOrder.getId(), item.getId(), 2, item.getPrice())))
                .then();

        setupData.thenMany(orderService.findByUserId(1L))
                .collectList()
                .doOnNext(orders -> assertThat(orders).hasSize(1))
                .block();
    }

    @Test
    void findByUserId_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .then();

        setupData.thenMany(orderService.findByUserId(1L))
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
                    order.setUserId(1L);
                    order.setOrderDate(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .flatMap(createdOrder -> orderItemRepository.save(new OrderItem(createdOrder.getId(), item.getId(), 2, item.getPrice()))
                        .thenReturn(createdOrder));

        setupOrderData.flatMap(expectOrder -> orderService.findById(expectOrder.getId())
                .flatMap(actualOrder -> orderService.getItems(actualOrder.getId())
                        .collectList()
                        .doOnNext(actualOrderItems -> assertAll(
                                () -> assertEquals(expectOrder.getId(), actualOrder.getId()),
                                () -> assertEquals(expectOrder.getUserId(), actualOrder.getUserId()),
                                () -> assertThat(actualOrder.getOrderDate()).isCloseTo(expectOrder.getOrderDate(), within(1, ChronoUnit.MICROS)),
                                () -> assertEquals(1, actualOrderItems.size())
                        )))).block();
    }

    @Test
    void findById_empty() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupOrderData = itemRepository.save(item)
                .flatMap(createdItem -> {
                    var order = new Order();
                    order.setUserId(1L);
                    order.setOrderDate(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .flatMap(createdOrder -> orderItemRepository.save(new OrderItem(createdOrder.getId(), item.getId(), 2, item.getPrice()))
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
                    var cartItem = new CartItem(1L, createdItem.getId(), 2);
                    return cartItemRepository.save(cartItem);
                })
                .then();

        setupData.then(orderService.create(1L))
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
                                .satisfies(firstOrderItem -> assertThat(firstOrderItem.getUnitPrice()).isEqualTo(10_000))
                ))
                .block();
    }

    @Test
    void create_emptyCart() {
        var item = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(item)
                .then();

        StepVerifier.create(setupData.then(orderService.create(1L)))
                .expectError(EmptyCartException.class)
                .verify();
    }

}
