package com.github.dgaponov99.practicum.mymarket.web.service;

import com.github.dgaponov99.practicum.mymarket.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.exception.ImageItemNotFoundException;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public Mono<ItemView> getItem(long id) {
        var item = itemService.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        var itemCartCount = cartService.countByItemId(id);
        return Mono.just(toItemView(item, itemCartCount));
    }

    public Mono<Resource> getItemImageResource(long itemId) {
        try {
            return Mono.just(new InputStreamResource(itemImageService.getImage(itemId)));
        } catch (ImageItemNotFoundException e) {
            return Mono.empty();
        }
    }

    @Transactional(readOnly = true)
    public Flux<ItemView> getCartItems() {
        return Flux.fromStream(cartService.getCartItems().stream().map(cartItem -> {
            var item = itemService.findById(cartItem.getItemId()).orElseThrow(() -> new ItemNotFoundException(cartItem.getItemId()));
            return toItemView(item, cartItem.getCount());
        }));
    }

    @Transactional(readOnly = true)
    public Mono<ItemsPageView> search(String searchText, int pageNumber, int pageSize, ItemsSortBy sortBy) {
        var page = itemService.search(searchText, pageNumber - 1, pageSize, sortBy);
        var paging = toPagingView(page);
        var itemViewList = page.getContent().stream()
                .map(item -> {
                    var itemCartCount = cartService.countByItemId(item.getId());
                    return toItemView(item, itemCartCount);
                }).toList();
        return Mono.just(new ItemsPageView(itemViewList, paging));
    }

    @Transactional(readOnly = true)
    public Mono<OrderView> getOrder(long id) {
        var order = orderService.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        return Mono.just(toOrderView(order));
    }

    @Transactional(readOnly = true)
    public Flux<OrderView> getOrders() {
        return Flux.fromStream(orderService.findAll().stream().map(this::toOrderView));
    }

    public Mono<Void> cartAction(long itemId, CartAction action) {
        return Mono.fromRunnable(() -> {
            switch (action) {
                case PLUS -> cartService.incrementItem(itemId);
                case MINUS -> {
                    try {
                        cartService.decrementItem(itemId);
                    } catch (CartItemNotFoundException ignore) {
                    }
                }
            }
        });
    }

    @Transactional
    public Mono<Long> buy() {
        return Mono.just(orderService.create().getId());
    }

    public long calculateTotalPrice(List<ItemView> items) {
        return items.stream().mapToLong(itemView -> itemView.price() * itemView.count()).sum();
    }

    private OrderView toOrderView(Order order) {
        OrderView orderView = new OrderView();
        orderView.setId(order.getId());
        orderView.setItems(order.getOrderItems().stream()
                .map(orderItem -> toItemView(orderItem.getItem(), orderItem.getCount()))
                .toList());
        orderView.setTotalSum(calculateTotalPrice(orderView.items()));
        return orderView;
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

    private PagingView toPagingView(Page<?> page) {
        var paging = new PagingView();
        paging.setPageNumber(page.getNumber() + 1);
        paging.setPageSize(page.getSize());
        paging.setHasPrevious(page.hasPrevious());
        paging.setHasNext(page.hasNext());
        return paging;
    }

}
