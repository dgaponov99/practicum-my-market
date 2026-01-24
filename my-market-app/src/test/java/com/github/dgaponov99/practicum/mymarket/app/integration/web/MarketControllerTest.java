package com.github.dgaponov99.practicum.mymarket.app.integration.web;

import com.github.dgaponov99.practicum.mymarket.app.client.api.AccountApi;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AccountDTO;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AmountDTO;
import com.github.dgaponov99.practicum.mymarket.app.config.CacheProperties;
import com.github.dgaponov99.practicum.mymarket.app.config.MarketViewProperties;
import com.github.dgaponov99.practicum.mymarket.app.event.DomainEventBus;
import com.github.dgaponov99.practicum.mymarket.app.event.ItemChangeEvent;
import com.github.dgaponov99.practicum.mymarket.app.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.integration.PostgreSQLTestcontainer;
import com.github.dgaponov99.practicum.mymarket.app.integration.RedisTestcontainer;
import com.github.dgaponov99.practicum.mymarket.app.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.CartItem;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Order;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.OrderItem;
import com.github.dgaponov99.practicum.mymarket.app.service.CartService;
import com.github.dgaponov99.practicum.mymarket.app.service.ItemImageService;
import com.github.dgaponov99.practicum.mymarket.app.service.ItemService;
import com.github.dgaponov99.practicum.mymarket.app.service.OrderService;
import com.github.dgaponov99.practicum.mymarket.app.web.controller.MarketController;
import com.github.dgaponov99.practicum.mymarket.app.web.service.MarketViewService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

@Slf4j
@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
@ImportTestcontainers({RedisTestcontainer.class, PostgreSQLTestcontainer.class})
public class MarketControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private CacheProperties cacheProperties;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private DomainEventBus domainEventBus;

    @MockitoBean
    ItemService itemService;
    @MockitoBean
    CartService cartService;
    @MockitoBean
    OrderService orderService;
    @MockitoBean
    ItemImageService itemImageService;
    @MockitoBean
    AccountApi accountApi;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(Cache::clear);
    }

    @ParameterizedTest
    @CsvSource({
            "/", "/items",
    })
    void items_empty() {
        when(itemService.searchCount(isNull())).thenReturn(Mono.just(0));
        when(itemService.search(isNull(), anyInt(), anyInt(), any())).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML);

        verify(itemService, times(1)).search(null, 0, 5, ItemsSortBy.NO);
        verify(itemService, times(1)).searchCount(null);
        verifyNoMoreInteractions(itemService);
    }

    @Test
    void items_notEmpty() {
        when(itemService.searchCount(anyString())).thenReturn(Mono.just(10));
        when(itemService.search(anyString(), anyInt(), anyInt(), any())).thenReturn(Flux.fromIterable(
                List.of(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false),
                        new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false),
                        new Item(1L, "Intel Core i3", "Intel Core i3", 1900, false))));
        when(cartService.countByItemId(anyLong())).thenReturn(Mono.just(0), Mono.just(0), Mono.just(1));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("search", "core")
                        .queryParam("pageSize", "3")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        verify(itemService, times(1)).search("core", 0, 3, ItemsSortBy.NO);
        verify(itemService, times(1)).searchCount("core");
        verify(cartService, times(3)).countByItemId(anyLong());
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void itemCartAction_increment() {
        when(cartService.incrementItem(anyLong())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("id", "1")
                        .with("action", "PLUS")
                        .with("search", "core"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/items?search=core");

        verify(cartService, times(1)).incrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void itemCartAction_decrement() {
        when(cartService.decrementItem(anyLong())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("id", "1")
                        .with("action", "MINUS")
                        .with("search", "core"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/items?search=core");

        verify(cartService, times(1)).decrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void itemCartAction_decrement_cartItemNotFoundException() {
        when(cartService.decrementItem(anyLong())).thenReturn(Mono.error(new CartItemNotFoundException(1L)));

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("id", "1")
                        .with("action", "MINUS")
                        .with("search", "core"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/items?search=core");

        verify(cartService, times(1)).decrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void item_success() {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));
        when(cartService.countByItemId(anyLong())).thenReturn(Mono.just(1));

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        verify(itemService, times(1)).findById(1L);
        verify(cartService, times(1)).countByItemId(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void item_cached() throws InterruptedException {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));
        when(cartService.countByItemId(anyLong())).thenReturn(Mono.just(1));

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        TimeUnit.MILLISECONDS.sleep(Math.min(200, cacheProperties.getCacheRedisSeconds() * 1000 / 2));

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        verify(itemService, times(1)).findById(1L);
        verify(cartService, times(1)).countByItemId(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void item_cacheExpired() throws InterruptedException {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));
        when(cartService.countByItemId(anyLong())).thenReturn(Mono.just(1));

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        TimeUnit.MILLISECONDS.sleep(cacheProperties.getCacheRedisSeconds() * 1000 + 2);

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        verify(itemService, times(2)).findById(1L);
        verify(cartService, times(2)).countByItemId(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void item_cacheUpdated() throws InterruptedException {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));
        when(cartService.countByItemId(anyLong())).thenReturn(Mono.just(1));

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        domainEventBus.publish(new ItemChangeEvent(1L));

        TimeUnit.MILLISECONDS.sleep(100);

        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));


        verify(itemService, times(2)).findById(1L);
        verify(cartService, times(2)).countByItemId(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void item_increment() {
        when(cartService.incrementItem(anyLong())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/items/{id}", 1)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/items/1");

        verify(cartService, times(1)).incrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void cart_enableBuy() {
        when(cartService.getCartItems()).thenReturn(Flux.just(new CartItem(1L, 3, false), new CartItem(2L, 2, false)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)),
                Mono.just(new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false))
        );
        when(accountApi.getAccount()).thenReturn(Mono.just(new AccountDTO().balance(1000000L)));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        verify(cartService, times(1)).getCartItems();
        verify(itemService, times(2)).findById(anyLong());
        verify(accountApi, times(1)).getAccount();
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void cart_notEnableBuy_insufficientBalance() {
        when(cartService.getCartItems()).thenReturn(Flux.just(new CartItem(1L, 3, false), new CartItem(2L, 2, false)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)),
                Mono.just(new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false))
        );
        when(accountApi.getAccount()).thenReturn(Mono.just(new AccountDTO().balance(500000L)));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")))
                .value(html -> assertTrue(html.contains("Не достаточно средств для совершения покупки")));

        verify(cartService, times(1)).getCartItems();
        verify(itemService, times(2)).findById(anyLong());
        verify(accountApi, times(1)).getAccount();
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void cart_notEnableBuy_paymentServiceRefused() {
        when(cartService.getCartItems()).thenReturn(Flux.just(new CartItem(1L, 3, false), new CartItem(2L, 2, false)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)),
                Mono.just(new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false))
        );
        when(accountApi.getAccount()).thenReturn(Mono.error(new WebClientRequestException(
                new ConnectException("Connection refused"),
                HttpMethod.POST,
                URI.create("http://payment"),
                HttpHeaders.EMPTY)));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")))
                .value(html -> assertTrue(html.contains("Сервис платежей временно недоступен")));

        verify(cartService, times(1)).getCartItems();
        verify(itemService, times(2)).findById(anyLong());
        verify(accountApi, times(1)).getAccount();
        verifyNoMoreInteractions(itemService, cartService, accountApi);
    }

    @Test
    void cartItemsAction_increment() {
        when(cartService.incrementItem(anyLong())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("action", "PLUS")
                        .with("id", "1"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/cart/items");

        verify(cartService, times(1)).incrementItem(1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void orders_empty() {
        when(orderService.findAll()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findAll();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void orders_notEmpty() {
        when(orderService.findAll()).thenReturn(Flux.just(new Order(1L, LocalDateTime.now())));
        when(orderService.getItems(anyLong())).thenReturn(Flux.just(new OrderItem(1L, 1L, 2)));
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(1L, "Товар", "Описание товара", 10_000, false)));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findAll();
        verify(orderService, times(1)).getItems(1L);
        verify(itemService, times(1)).findById(1L);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void order_success() {
        when(orderService.findById(anyLong())).thenReturn(Mono.just(new Order(1L, LocalDateTime.now())));
        when(orderService.getItems(anyLong())).thenReturn(Flux.just(new OrderItem(1L, 1L, 2)));
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(1L, "Товар", "Описание товара", 10_000, false)));

        webTestClient.get()
                .uri("/orders/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Товар")));

        verify(orderService, times(1)).findById(1L);
        verify(orderService, times(1)).getItems(1L);
        verify(itemService, times(1)).findById(1L);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void buy_success() {
        when(cartService.getCartItems()).thenReturn(Flux.just(new CartItem(1L, 3, false), new CartItem(2L, 2, false)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)),
                Mono.just(new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false))
        );
        when(orderService.create()).thenReturn(Mono.just(new Order(1L, LocalDateTime.now())));
        when(accountApi.debit(any())).thenReturn(Mono.just(new AccountDTO().balance(30000L)));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/orders/1?newOrder=true");

        verify(cartService, times(1)).getCartItems();
        verify(itemService, times(2)).findById(anyLong());
        verify(orderService, times(1)).create();
        verify(accountApi, times(1)).debit(new AmountDTO().amount(950000L));
        verifyNoMoreInteractions(orderService, cartService, itemService, accountApi);
    }

}
