package com.github.dgaponov99.practicum.mymarket.app.web.service;

import com.github.dgaponov99.practicum.mymarket.app.client.api.AccountApi;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AccountDTO;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AmountDTO;
import com.github.dgaponov99.practicum.mymarket.app.config.CacheProperties;
import com.github.dgaponov99.practicum.mymarket.app.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.exception.ItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.exception.OrderNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.app.service.*;
import com.github.dgaponov99.practicum.mymarket.app.web.CartAction;
import com.github.dgaponov99.practicum.mymarket.app.web.mapper.MarketViewMapper;
import com.github.dgaponov99.practicum.mymarket.app.web.view.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketViewService {

    private final ItemService itemService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ItemImageService itemImageService;
    private final MarketViewMapper marketViewMapper;
    private final AccountApi accountApi;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final CacheProperties cacheProperties;
    private final UserService userService;

    public Mono<ItemView> getItem(long id) {
        var cacheKey = "itemView:%d".formatted(id);
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(ItemView.class)
                .doOnNext(v -> log.debug("VALUE: {}", v))
                .switchIfEmpty(Mono.defer(() -> itemService.findById(id)
                        .map(marketViewMapper::toItemView)
                        .flatMap(value -> redisTemplate.opsForValue()
                                .set(cacheKey, value, cacheProperties.getCacheRedisDuration())
                                .doOnSuccess(v -> log.debug("REDIS SET OK"))
                                .doOnError(e -> log.error("REDIS SET FAIL", e))
                                .thenReturn(value)
                        ))
                );
    }

    public Flux<DataBuffer> getItemImageResource(long itemId, DataBufferFactory dataBufferFactory) {
        return itemImageService.getImage(itemId, dataBufferFactory);
    }

    public Mono<Map<Long, Integer>> getUserCartItems(long userId) {
        return cartService.getCartItems(userId)
                .collectMap(CartItem::getItemId, CartItem::getCount);
    }

    public Mono<CartPageView> getCartPageView(long userId) {
        return cartService.getCartItems(userId)
                .flatMap(cartItem ->
                        itemService.findById(cartItem.getItemId())
                                .switchIfEmpty(Mono.error(new ItemNotFoundException(cartItem.getItemId())))
                                .map(item -> marketViewMapper.toCartItemView(cartItem, item)))
                .collectList()
                .map(cartItemTuples -> marketViewMapper.toCartPageView(userId, cartItemTuples));
    }

    public Mono<EnableBuyView> enableBuy(long userId, long cartTotal) {
        return userService.findById(userId).flatMap(user -> accountApi.getAccount(user.getAccountId())
                .map(AccountDTO::getBalance)
                .flatMap(currentBalance -> {
                    if (currentBalance >= cartTotal * 100L) {
                        return Mono.just(new EnableBuyView(true, null));
                    } else {
                        return Mono.just(new EnableBuyView(false, "Не достаточно средств для совершения покупки"));
                    }
                })
                .onErrorResume(WebClientRequestException.class,
                        ex -> Mono.just(new EnableBuyView(false, "Невозможно совершить покупку. Сервис платежей временно недоступен."))));
    }

    public Mono<ItemsPageView> search(String searchText, int pageNumber, int pageSize, ItemsSortBy sortBy) {
        var cacheKey = "itemsPageView:%s:%d:%d:%s".formatted(searchText, pageNumber, pageSize, sortBy);
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(ItemsPageView.class)
                .switchIfEmpty(itemService.searchCount(searchText)
                        .flatMap(totalSearchCount ->
                                itemService.search(searchText, pageNumber - 1, pageSize, sortBy)
                                        .collectList()
                                        .map(items ->
                                                marketViewMapper.toItemPageView(items, pageNumber, pageSize, totalSearchCount, items.size()))
                        )
                        .flatMap(value -> redisTemplate.opsForValue()
                                .set(cacheKey, value, cacheProperties.getCacheRedisDuration())
                                .thenReturn(value)
                        ));
    }

    public Mono<OrderPageView> getOrder(long id) {
        var cacheKey = "orderView:%d".formatted(id);
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(OrderPageView.class)
                .switchIfEmpty(orderService.findById(id)
                        .switchIfEmpty(Mono.error(new OrderNotFoundException(id)))
                        .flatMap(order -> orderService.getItems(id)
                                .flatMap(orderItem -> itemService.findById(orderItem.getItemId())
                                        .map(item -> marketViewMapper.toOrderItemView(orderItem, item))
                                ).collectList()
                                .map(orderItemViews -> marketViewMapper.toOrderPageView(order, orderItemViews)))
                        .flatMap(value -> redisTemplate.opsForValue()
                                .set(cacheKey, value, cacheProperties.getCacheRedisDuration())
                                .thenReturn(value)
                        ));
    }

    public Flux<OrderPageView> getOrders(long userId) {
        return orderService.findIdsByUserId(userId)
                .flatMap(this::getOrder);
    }

    public Mono<Void> cartAction(long userId, long itemId, CartAction action) {
        return switch (action) {
            case PLUS -> cartService.incrementItem(userId, itemId);
            case MINUS -> cartService.decrementItem(userId, itemId).onErrorComplete(CartItemNotFoundException.class);
        };
    }

    public Mono<Long> buy(long userId) {
        return userService.findById(userId).flatMap(user -> getCartPageView(userId)
                .map(CartPageView::getTotalPrice)
                .flatMap(total ->
                        accountApi.debit(user.getAccountId(), new AmountDTO().amount(total * 100))
                                .then(orderService.create(userId)
                                        .map(Order::getId)
                                        .onErrorResume(ex ->
                                                accountApi.credit(user.getAccountId(), new AmountDTO().amount(total * 100))
                                                        .then(Mono.error(ex))
                                        )
                                )
                ));
    }

}
