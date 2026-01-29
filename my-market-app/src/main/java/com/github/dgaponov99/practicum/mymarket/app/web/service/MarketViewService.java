package com.github.dgaponov99.practicum.mymarket.app.web.service;

import com.github.dgaponov99.practicum.mymarket.app.client.api.AccountApi;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AccountDTO;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AmountDTO;
import com.github.dgaponov99.practicum.mymarket.app.config.CacheProperties;
import com.github.dgaponov99.practicum.mymarket.app.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.exception.ItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.exception.OrderNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.app.service.CartService;
import com.github.dgaponov99.practicum.mymarket.app.service.ItemImageService;
import com.github.dgaponov99.practicum.mymarket.app.service.ItemService;
import com.github.dgaponov99.practicum.mymarket.app.service.OrderService;
import com.github.dgaponov99.practicum.mymarket.app.web.CartAction;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketViewService {

    private final ItemService itemService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ItemImageService itemImageService;
    private final AccountApi accountApi;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final CacheProperties cacheProperties;

    public Mono<ItemView> getItem(long id) {
        var cacheKey = "itemView:%d".formatted(id);
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .doOnNext(v -> log.debug("VALUE: {}", v))
                .switchIfEmpty(Mono.zip(Mono.defer(() -> itemService.findById(id)),
                                        Mono.defer(() -> cartService.countByItemId(id))
                                )
                                .map(tuple -> toItemView(tuple.getT1(), tuple.getT2()))
                                .flatMap(value -> redisTemplate.opsForValue()
                                        .set(cacheKey, value, cacheProperties.getCacheRedisDuration())
                                        .doOnSuccess(v -> log.debug("REDIS SET OK"))
                                        .doOnError(e -> log.error("REDIS SET FAIL", e))
                                        .thenReturn(value)
                                )
                )
                .cast(ItemView.class);
    }

    public Flux<DataBuffer> getItemImageResource(long itemId, DataBufferFactory dataBufferFactory) {
        return itemImageService.getImage(itemId, dataBufferFactory);
    }

    public Flux<ItemView> getCartItems() {
        return cartService.getCartItems()
                .flatMap(cartItem ->
                        itemService.findById(cartItem.getItemId())
                                .switchIfEmpty(Mono.error(new ItemNotFoundException(cartItem.getItemId())))
                                .map(item -> toItemView(item, cartItem.getCount())));
    }

    public Mono<EnableBuyView> enableBuy(long cartTotal) {
        return accountApi.getAccount()
                .map(AccountDTO::getBalance)
                .flatMap(currentBalance -> {
                    if (currentBalance >= cartTotal * 100L) {
                        return Mono.just(new EnableBuyView(true, null));
                    } else {
                        return Mono.just(new EnableBuyView(false, "Не достаточно средств для совершения покупки"));
                    }
                })
                .onErrorResume(WebClientRequestException.class,
                        ex -> Mono.just(new EnableBuyView(false, "Невозможно совершить покупку. Сервис платежей временно недоступен.")));
    }

    public Mono<ItemsPageView> search(String searchText, int pageNumber, int pageSize, ItemsSortBy sortBy) {
        var cacheKey = "itemsPageView:%s:%d:%d:%s".formatted(searchText, pageNumber, pageSize, sortBy);
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(ItemsPageView.class)
                .switchIfEmpty(itemService.searchCount(searchText).flatMap(totalSearchCount ->
                                itemService.search(searchText, pageNumber - 1, pageSize, sortBy)
                                        .flatMap(item -> cartService.countByItemId(item.getId())
                                                .map(itemCartCount -> toItemView(item, itemCartCount))
                                        )
                                        .collectList()
                                        .map(itemViews ->
                                                new ItemsPageView(itemViews,
                                                        toPagingView(pageNumber, pageSize, totalSearchCount, itemViews.size())
                                                )))
                        .flatMap(value -> redisTemplate.opsForValue()
                                .set(cacheKey, value, cacheProperties.getCacheRedisDuration())
                                .thenReturn(value)
                        ));
    }

    public Mono<OrderView> getOrder(long id) {
        var cacheKey = "orderView:%d".formatted(id);
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(OrderView.class)
                .switchIfEmpty(orderService.findById(id)
                        .switchIfEmpty(Mono.error(new OrderNotFoundException(id)))
                        .flatMap(this::flatMapOrderView)
                        .flatMap(value -> redisTemplate.opsForValue()
                                .set(cacheKey, value, cacheProperties.getCacheRedisDuration())
                                .thenReturn(value)
                        ));
    }

    public Flux<OrderView> getOrders() {
        var cacheKey = "ordersView";
        return redisTemplate.opsForList()
                .range(cacheKey, 0, -1)
                .cast(OrderView.class)
                .switchIfEmpty(orderService.findAll()
                        .flatMap(this::flatMapOrderView)
                        .collectList()
                        .flatMapMany(values -> {
                            if (values.isEmpty()) {
                                return Flux.empty();
                            }
                            return redisTemplate.opsForList()
                                    .rightPushAll(cacheKey, values.toArray())
                                    .then(redisTemplate.expire(cacheKey, cacheProperties.getCacheRedisDuration()))
                                    .thenMany(Flux.fromIterable(values));
                        })
                );
    }

    public Mono<Void> cartAction(long itemId, CartAction action) {
        return switch (action) {
            case PLUS -> cartService.incrementItem(itemId);
            case MINUS -> cartService.decrementItem(itemId).onErrorComplete(CartItemNotFoundException.class);
        };
    }

    public Mono<Long> buy() {
        return getCartItems().collectList()
                .map(this::calculateTotalPrice)
                .flatMap(total ->
                        accountApi.debit(new AmountDTO().amount(total * 100))
                                .then(orderService.create()
                                        .map(Order::getId)
                                        .onErrorResume(ex ->
                                                accountApi.credit(new AmountDTO().amount(total * 100))
                                                        .then(Mono.error(ex))
                                        )
                                )
                );
    }

    public long calculateTotalPrice(List<ItemView> itemViews) {
        return itemViews.stream().mapToLong(itemView -> itemView.getPrice() * itemView.getCount()).sum();
    }

    private Mono<OrderView> flatMapOrderView(Order order) {
        return orderService.getItems(order.getId())
                .flatMap(orderItem -> itemService.findById(orderItem.getItemId())
                        .map(item -> toItemView(item, orderItem.getCount()))
                )
                .collectList()
                .map(itemViews -> {
                    var orderView = new OrderView();
                    orderView.setId(order.getId());
                    orderView.setItems(itemViews);
                    orderView.setTotalSum(calculateTotalPrice(itemViews));
                    return orderView;
                });
    }

    private ItemView toItemView(Item item, int count) {
        var itemView = new ItemView();
        itemView.setId(item.getId());
        itemView.setTitle(item.getTitle());
        itemView.setDescription(item.getDescription());
        itemView.setPrice(item.getPrice());
        itemView.setCount(count);
        return itemView;
    }

    private PagingView toPagingView(int pageNumber, int pageSize, int totalCount, int contentCount) {
        var paging = new PagingView();
        paging.setPageNumber(pageNumber);
        paging.setPageSize(pageSize);
        paging.setHasPrevious(pageNumber > 1);
        paging.setHasNext((pageNumber - 1) * pageSize + contentCount < totalCount);
        return paging;
    }

}
