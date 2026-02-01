package com.github.dgaponov99.practicum.mymarket.app.service;

import com.github.dgaponov99.practicum.mymarket.app.event.DomainEventBus;
import com.github.dgaponov99.practicum.mymarket.app.event.OrderChangeEvent;
import com.github.dgaponov99.practicum.mymarket.app.exception.EmptyCartException;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.OrderItem;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.OrderItemRepository;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ItemService itemService;
    private final DomainEventBus domainEventBus;

    public Mono<Order> findById(long id) {
        return orderRepository.findById(id);
    }

    public Flux<Long> findIdsByUserId(long userId) {
        return orderRepository.findOrderIdsByUserId(userId);
    }

    public Flux<Order> findByUserId(long userId) {
        return orderRepository.findOrdersByUserId(userId);
    }

    public Flux<OrderItem> getItems(long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Mono<Order> create(long userId) {
        return cartService.getCartItems(userId)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new EmptyCartException(userId));
                    }

                    var order = new Order();
                    order.setUserId(userId);
                    order.setOrderDate(LocalDateTime.now());

                    return orderRepository.save(order)
                            .flatMap(createdOrder ->
                                    Flux.fromIterable(cartItems)
                                            .flatMap(cartItem ->
                                                    itemService.findById(cartItem.getItemId())
                                                            .flatMap(item -> orderItemRepository.save(
                                                                    new OrderItem(createdOrder.getId(),
                                                                            cartItem.getItemId(),
                                                                            cartItem.getCount(),
                                                                            item.getPrice())
                                                            )))
                                            .then(cartService.deleteCart(userId))
                                            .then(Mono.just(createdOrder)));
                })
                .doOnSuccess(order -> domainEventBus.publish(new OrderChangeEvent(order.getId(), userId)));
    }

}
