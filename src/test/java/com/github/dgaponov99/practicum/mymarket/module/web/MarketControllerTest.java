package com.github.dgaponov99.practicum.mymarket.module.web;

import com.github.dgaponov99.practicum.mymarket.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.service.CartService;
import com.github.dgaponov99.practicum.mymarket.service.ItemImageService;
import com.github.dgaponov99.practicum.mymarket.service.ItemService;
import com.github.dgaponov99.practicum.mymarket.service.OrderService;
import com.github.dgaponov99.practicum.mymarket.web.controller.MarketController;
import com.github.dgaponov99.practicum.mymarket.web.service.MarketViewService;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@WebMvcTest(controllers = MarketController.class)
@Import(MarketViewService.class)
@AutoConfigureMockMvc
public class MarketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    ItemService itemService;
    @MockitoBean
    CartService cartService;
    @MockitoBean
    OrderService orderService;
    @MockitoBean
    ItemImageService itemImageService;

    @ParameterizedTest
    @CsvSource({
            "/", "/items",
    })
    void items_empty() throws Exception {
        when(itemService.search(isNull(), anyInt(), anyInt(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));

        verify(itemService, times(1)).search(null, 0, 5, ItemsSortBy.NO);
        verifyNoMoreInteractions(itemService);
    }

    @Test
    void items_notEmpty() throws Exception {
        when(itemService.search(anyString(), anyInt(), anyInt(), any())).thenReturn(new PageImpl<>(
                List.of(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300),
                        new Item(2L, "Intel Core i7", "Intel Core i7", 1300),
                        new Item(1L, "Intel Core i3", "Intel Core i3", 1900)),
                PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "id")),
                10));
        when(cartService.countByItemId(anyLong())).thenReturn(0, 0, 1);

        mockMvc.perform(get("/items")
                        .queryParam("search", "core")
                        .queryParam("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(StringContains.containsString("Intel Core i7 4th gen")));

        verify(itemService, times(1)).search("core", 0, 3, ItemsSortBy.NO);
        verify(cartService, times(3)).countByItemId(anyLong());
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void itemCartAction_increment() throws Exception {
        doNothing().when(cartService).incrementItem(anyLong());

        mockMvc.perform(post("/items")
                        .queryParam("id", "1")
                        .queryParam("action", "PLUS")
                        .queryParam("search", "core"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/items?search=core"));

        verify(cartService, times(1)).incrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void itemCartAction_decrement() throws Exception {
        doNothing().when(cartService).decrementItem(anyLong());

        mockMvc.perform(post("/items")
                        .queryParam("id", "1")
                        .queryParam("action", "MINUS")
                        .queryParam("search", "core"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/items?search=core"));

        verify(cartService, times(1)).decrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void itemCartAction_decrement_cartItemNotFoundException() throws Exception {
        doThrow(CartItemNotFoundException.class).when(cartService).decrementItem(anyLong());

        mockMvc.perform(post("/items")
                        .queryParam("id", "1")
                        .queryParam("action", "MINUS")
                        .queryParam("search", "core"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/items?search=core"));

        verify(cartService, times(1)).decrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void item_success() throws Exception {
        when(itemService.findById(anyLong())).thenReturn(Optional.of(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300)));
        when(cartService.countByItemId(anyLong())).thenReturn(1);

        mockMvc.perform(get("/items/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(StringContains.containsString("Intel Core i7 4th gen")));

        verify(itemService, times(1)).findById(1L);
        verify(cartService, times(1)).countByItemId(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void item_increment() throws Exception {
        doNothing().when(cartService).incrementItem(anyLong());

        mockMvc.perform(post("/items/{id}", 1)
                        .queryParam("action", "PLUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/items/1"));

        verify(cartService, times(1)).incrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void cart() throws Exception {
        when(cartService.getCartItems()).thenReturn(List.of(new CartItem(1L, 3), new CartItem(2L, 2)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Optional.of(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300)),
                Optional.of(new Item(2L, "Intel Core i7", "Intel Core i7", 1300))
        );

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(StringContains.containsString("Intel Core i7 4th gen")));

        verify(cartService, times(1)).getCartItems();
        verify(itemService, times(2)).findById(anyLong());
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void cartItemsAction_increment() throws Exception {
        doNothing().when(cartService).incrementItem(anyLong());

        mockMvc.perform(post("/cart/items")
                        .queryParam("id", "1")
                        .queryParam("action", "PLUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/cart/items"));

        verify(cartService, times(1)).incrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void orders_empty() throws Exception {
        when(orderService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));

        verify(orderService, times(1)).findAll();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void orders_notEmpty() throws Exception {
        var order = new Order();
        order.setId(1L);
        order.setOrderDate(Instant.now());
        order.addItem(new Item(1L, "Товар", "Описание товара", 10_000), 2);
        when(orderService.findAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));

        verify(orderService, times(1)).findAll();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void order_success() throws Exception {
        var order = new Order();
        order.setId(1L);
        order.setOrderDate(Instant.now());
        order.addItem(new Item(1L, "Товар", "Описание товара", 10_000), 2);

        when(orderService.findById(anyLong())).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(StringContains.containsString("Товар")));

        verify(orderService, times(1)).findById(1L);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void buy_success() throws Exception {
        var order = new Order();
        order.setId(1L);
        order.setOrderDate(Instant.now());
        order.addItem(new Item(1L, "Товар", "Описание товара", 10_000), 2);
        when(orderService.create()).thenReturn(order);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/orders/1?newOrder=true"));

        verify(orderService, times(1)).create();
        verifyNoMoreInteractions(orderService);
    }

}
