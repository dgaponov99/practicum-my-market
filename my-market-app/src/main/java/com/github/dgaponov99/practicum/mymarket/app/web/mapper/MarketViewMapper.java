package com.github.dgaponov99.practicum.mymarket.app.web.mapper;

import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.OrderItem;
import com.github.dgaponov99.practicum.mymarket.app.web.view.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketViewMapper {

    public ItemView toItemView(Item item) {
        var itemView = new ItemView();
        itemView.setId(item.getId());
        itemView.setTitle(item.getTitle());
        itemView.setDescription(item.getDescription());
        itemView.setPrice(item.getPrice());
        return itemView;
    }

    public ItemsPageView toItemPageView(List<Item> items, int pageNumber, int pageSize, int totalCount, int contentCount) {
        var itemsPageView = new ItemsPageView();
        itemsPageView.setItems(items.stream().map(this::toItemView).toList());
        itemsPageView.setPaging(toPagingView(pageNumber, pageSize, totalCount, contentCount));
        return itemsPageView;
    }

    public CartItemView toCartItemView(CartItem cartItem, Item item) {
        var cartItemView = new CartItemView();
        cartItemView.setUserId(cartItem.getUserId());
        cartItemView.setItem(toItemView(item));
        cartItemView.setCount(cartItem.getCount());
        cartItemView.setTotalPrice(cartItem.getCount() * item.getPrice());
        return cartItemView;
    }

    public CartPageView toCartPageView(long userId, List<CartItemView> cartItemViews) {
        var cartPageView = new CartPageView();
        cartPageView.setUserId(userId);
        cartPageView.setCartItems(cartItemViews);
        cartPageView.setTotalPrice(cartPageView.getCartItems().stream().mapToLong(CartItemView::getTotalPrice).sum());
        return cartPageView;
    }

    public OrderItemView toOrderItemView(OrderItem orderItem, Item item) {
        var orderItemView = new OrderItemView();
        orderItemView.setOrderId(orderItem.getOrderId());
        orderItemView.setItem(toItemView(item));
        orderItemView.setCount(orderItem.getCount());
        orderItemView.setUnitPrice(orderItem.getUnitPrice());
        orderItemView.setTotalPrice(orderItem.getCount() * orderItem.getUnitPrice());
        return orderItemView;
    }

    public OrderPageView toOrderPageView(Order order, List<OrderItemView> orderItemViews) {
        var orderPageView = new OrderPageView();
        orderPageView.setId(order.getId());
        orderPageView.setUserId(order.getUserId());
        orderPageView.setItems(orderItemViews);
        orderPageView.setTotalPrice(orderItemViews.stream().mapToLong(OrderItemView::getTotalPrice).sum());
        return orderPageView;
    }

    public PagingView toPagingView(int pageNumber, int pageSize, int totalCount, int contentCount) {
        var paging = new PagingView();
        paging.setPageNumber(pageNumber);
        paging.setPageSize(pageSize);
        paging.setHasPrevious(pageNumber > 1);
        paging.setHasNext((pageNumber - 1) * pageSize + contentCount < totalCount);
        return paging;
    }

}
