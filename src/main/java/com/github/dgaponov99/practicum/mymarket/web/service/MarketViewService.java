package com.github.dgaponov99.practicum.mymarket.web.service;

import com.github.dgaponov99.practicum.mymarket.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.exception.ItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.exception.OrderNotFoundException;
import com.github.dgaponov99.practicum.mymarket.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.service.CartService;
import com.github.dgaponov99.practicum.mymarket.service.ItemImageService;
import com.github.dgaponov99.practicum.mymarket.service.ItemService;
import com.github.dgaponov99.practicum.mymarket.service.OrderService;
import com.github.dgaponov99.practicum.mymarket.web.CartAction;
import com.github.dgaponov99.practicum.mymarket.web.view.ItemView;
import com.github.dgaponov99.practicum.mymarket.web.view.ItemsPageView;
import com.github.dgaponov99.practicum.mymarket.web.view.OrderView;
import com.github.dgaponov99.practicum.mymarket.web.view.PagingView;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketViewService {

    private final ItemService itemService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ItemImageService itemImageService;

    public Mono<ItemView> getItem(long id) {
        return Mono.zip(itemService.findById(id),
                        cartService.countByItemId(id)
                )
                .map(tuple -> toItemView(tuple.getT1(), tuple.getT2()));
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

    public Mono<ItemsPageView> search(String searchText, int pageNumber, int pageSize, ItemsSortBy sortBy) {
        return itemService.searchCount(searchText).flatMap(totalSearchCount ->
                        itemService.search(searchText, pageNumber - 1, pageSize, sortBy)
                                .flatMap(item -> cartService.countByItemId(item.getId())
                                        .map(itemCartCount -> toItemView(item, itemCartCount))
                                )
                                .collectList()
                                .map(itemViews ->
                                        new ItemsPageView(itemViews,
                                                toPagingView(pageNumber, pageSize, totalSearchCount, itemViews.size())
                                        )));
    }

    public Mono<OrderView> getOrder(long id) {
        return orderService.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(id)))
                .flatMap(this::flatMapOrderView);
    }

    public Flux<OrderView> getOrders() {
        return orderService.findAll().flatMap(this::flatMapOrderView);
    }

    public Mono<Void> cartAction(long itemId, CartAction action) {
        return switch (action) {
            case PLUS -> cartService.incrementItem(itemId);
            case MINUS -> cartService.decrementItem(itemId).onErrorComplete(CartItemNotFoundException.class);
        };
    }

    public Mono<Long> buy() {
        return orderService.create().map(Order::getId);
    }

    public long calculateTotalPrice(List<ItemView> itemViews) {
        return itemViews.stream().mapToLong(itemView -> itemView.price() * itemView.count()).sum();
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
