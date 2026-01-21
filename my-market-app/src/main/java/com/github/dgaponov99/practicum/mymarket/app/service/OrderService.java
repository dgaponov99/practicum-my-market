package com.github.dgaponov99.practicum.mymarket.app.service;

import com.github.dgaponov99.practicum.mymarket.app.exception.EmptyCartException;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.OrderItem;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.CartItemRepository;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.OrderItemRepository;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    public Mono<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Flux<Order> findAll() {
        return orderRepository.findAll();
    }

    public Flux<OrderItem> getItems(long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Mono<Order> create() {
        var cartItemFlux = cartItemRepository.findAll().cache();
        return cartItemFlux.hasElements()
                .flatMap(hasElements -> {
                    if (!hasElements) {
                        return Mono.error(new EmptyCartException());
                    }

                    var order = new Order();
                    order.setOrderDate(LocalDateTime.now());
                    return orderRepository.save(order)
                            .flatMap(createdOrder ->
                                    cartItemFlux.flatMap(cartItem ->
                                                    orderItemRepository.save(
                                                            new OrderItem(createdOrder.getId(), cartItem.getItemId(), cartItem.getCount())
                                                    ))
                                            .then(cartItemRepository.deleteAll())
                                            .then(Mono.just(createdOrder)));
                });
    }

}
