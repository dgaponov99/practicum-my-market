package com.github.dgaponov99.practicum.mymarket.app.integration.web;

import com.github.dgaponov99.practicum.mymarket.app.auth.IdentityUserDetails;
import com.github.dgaponov99.practicum.mymarket.app.client.api.AccountApi;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AccountDTO;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AmountDTO;
import com.github.dgaponov99.practicum.mymarket.app.config.CacheProperties;
import com.github.dgaponov99.practicum.mymarket.app.event.DomainEventBus;
import com.github.dgaponov99.practicum.mymarket.app.event.ItemChangeEvent;
import com.github.dgaponov99.practicum.mymarket.app.exception.CartItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.integration.PostgreSQLTestcontainer;
import com.github.dgaponov99.practicum.mymarket.app.integration.RedisTestcontainer;
import com.github.dgaponov99.practicum.mymarket.app.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.*;
import com.github.dgaponov99.practicum.mymarket.app.service.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ImportTestcontainers({RedisTestcontainer.class, PostgreSQLTestcontainer.class})
@ActiveProfiles("test")
public class MarketControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private CacheProperties cacheProperties;
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;
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
    @MockitoBean
    UserService userService;

    @BeforeEach
    void setUp() {
        redisTemplate.execute(connection ->
                connection.serverCommands().flushDb()
        ).then().block();
    }

    @ParameterizedTest
    @CsvSource({
            "/", "/items",
    })
    @WithAnonymousUser
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
    void items_authNotEmpty() {
        when(itemService.searchCount(anyString())).thenReturn(Mono.just(10));
        when(itemService.search(anyString(), anyInt(), anyInt(), any())).thenReturn(Flux.fromIterable(
                List.of(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false),
                        new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false),
                        new Item(1L, "Intel Core i3", "Intel Core i3", 1900, false))));
        when(cartService.getCartItems(anyLong())).thenReturn(Flux.just(new CartItem(1L, 3, 1)));

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .get()
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
        verify(cartService, times(1)).getCartItems(anyLong());
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    @WithAnonymousUser
    void items_NoAuthNotEmpty() {
        when(itemService.searchCount(anyString())).thenReturn(Mono.just(10));
        when(itemService.search(anyString(), anyInt(), anyInt(), any())).thenReturn(Flux.fromIterable(
                List.of(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false),
                        new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false),
                        new Item(1L, "Intel Core i3", "Intel Core i3", 1900, false))));

        webTestClient
                .get()
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
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void itemCartAction_increment() {
        when(cartService.incrementItem(anyLong(), anyLong())).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .mutateWith(csrf())
                .post()
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

        verify(cartService, times(1)).incrementItem(1L, 1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void itemCartAction_noCsrf() {
        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("id", "1")
                        .with("action", "PLUS")
                        .with("search", "core"))
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(cartService, never()).incrementItem(anyLong(), anyLong());
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @WithAnonymousUser
    void itemCartAction_noAuth() {
        webTestClient.mutateWith(csrf())
                .post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("id", "1")
                        .with("action", "PLUS")
                        .with("search", "core"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/login");

        verify(cartService, never()).incrementItem(anyLong(), anyLong());
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void itemCartAction_decrement() {
        when(cartService.decrementItem(anyLong(), anyLong())).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .mutateWith(csrf())
                .post()
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

        verify(cartService, times(1)).decrementItem(1L, 1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void itemCartAction_decrement_cartItemNotFoundException() {
        when(cartService.decrementItem(anyLong(), anyLong())).thenReturn(Mono.error(new CartItemNotFoundException(1L, 1L)));

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .mutateWith(csrf())
                .post()
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

        verify(cartService, times(1)).decrementItem(1L, 1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void item_success_auth() {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));
        when(cartService.getCartItems(anyLong())).thenReturn(Flux.just(new CartItem(1L, 1L, 1)));

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        verify(itemService, times(1)).findById(1L);
        verify(cartService, times(1)).getCartItems(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    @WithAnonymousUser
    void item_success_noAuth() {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));

        webTestClient
                .get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        verify(itemService, times(1)).findById(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    @WithAnonymousUser
    void item_cached() throws InterruptedException {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));

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
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    @WithAnonymousUser
    void item_cacheExpired() throws InterruptedException {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));

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
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    @WithAnonymousUser
    void item_cacheUpdated() throws InterruptedException {
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(3L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)));

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
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void item_increment() {
        when(cartService.incrementItem(anyLong(), anyLong())).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .mutateWith(csrf())
                .post()
                .uri("/items/{id}", 1)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/items/1");

        verify(cartService, times(1)).incrementItem(1L, 1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void cart_enableBuy() {
        when(cartService.getCartItems(anyLong())).thenReturn(Flux.just(new CartItem(1L, 1L, 3), new CartItem(1L, 2L, 2)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)),
                Mono.just(new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false))
        );
        when(userService.findById(1L)).thenReturn(Mono.just(new User(1L, "user", "password", 1L, false)));
        when(accountApi.getAccount(anyLong())).thenReturn(Mono.just(new AccountDTO().id(1L).balance(1000000L)));

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")));

        verify(cartService, times(1)).getCartItems(1L);
        verify(itemService, times(2)).findById(anyLong());
        verify(userService, times(1)).findById(1L);
        verify(accountApi, times(1)).getAccount(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void cart_notEnableBuy_insufficientBalance() {
        when(cartService.getCartItems(anyLong())).thenReturn(Flux.just(new CartItem(1L, 1L, 3), new CartItem(1L, 2L, 2)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)),
                Mono.just(new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false))
        );
        when(userService.findById(1L)).thenReturn(Mono.just(new User(1L, "user", "password", 1L, false)));
        when(accountApi.getAccount(anyLong())).thenReturn(Mono.just(new AccountDTO().id(1L).balance(500000L)));

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")))
                .value(html -> assertTrue(html.contains("Не достаточно средств для совершения покупки")));

        verify(cartService, times(1)).getCartItems(1L);
        verify(itemService, times(2)).findById(anyLong());
        verify(userService, times(1)).findById(1L);
        verify(accountApi, times(1)).getAccount(1L);
        verifyNoMoreInteractions(itemService, cartService);
    }

    @Test
    void cart_notEnableBuy_paymentServiceRefused() {
        when(cartService.getCartItems(anyLong())).thenReturn(Flux.just(new CartItem(1L, 1L, 3), new CartItem(1L, 2L, 2)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)),
                Mono.just(new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false))
        );
        when(userService.findById(1L)).thenReturn(Mono.just(new User(1L, "user", "password", 1L, false)));
        when(accountApi.getAccount(anyLong())).thenReturn(Mono.error(new WebClientRequestException(
                new ConnectException("Connection refused"),
                HttpMethod.POST,
                URI.create("http://payment"),
                HttpHeaders.EMPTY)));

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Intel Core i7 4th gen")))
                .value(html -> assertTrue(html.contains("Сервис платежей временно недоступен")));

        verify(cartService, times(1)).getCartItems(1L);
        verify(itemService, times(2)).findById(anyLong());
        verify(userService, times(1)).findById(1L);
        verify(accountApi, times(1)).getAccount(1L);
        verifyNoMoreInteractions(itemService, cartService, accountApi);
    }

    @Test
    void cartItemsAction_increment() {
        when(cartService.incrementItem(anyLong(), anyLong())).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .mutateWith(csrf())
                .post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("action", "PLUS")
                        .with("id", "1"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/cart/items");

        verify(cartService, times(1)).incrementItem(1L, 1L);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void orders_empty() {
        when(orderService.findIdsByUserId(anyLong())).thenReturn(Flux.empty());

        webTestClient
                .mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findIdsByUserId(1L);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void orders_notEmpty() {
        when(orderService.findIdsByUserId(anyLong())).thenReturn(Flux.just(1L));
        when(orderService.findById(anyLong())).thenReturn(Mono.just(new Order(1L, 1L, LocalDateTime.now())));
        when(orderService.getItems(anyLong())).thenReturn(Flux.just(new OrderItem(1L, 1L, 2, 10_000)));
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(1L, "Товар", "Описание товара", 10_000, false)));

        webTestClient
                .mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML);

        verify(orderService, times(1)).findIdsByUserId(1L);
        verify(orderService, times(1)).findById(1L);
        verify(orderService, times(1)).getItems(1L);
        verify(itemService, times(1)).findById(1L);
        verifyNoMoreInteractions(orderService, itemService);
    }

    @Test
    void order_success() {
        when(orderService.findById(anyLong())).thenReturn(Mono.just(new Order(1L, 1L, LocalDateTime.now())));
        when(orderService.getItems(anyLong())).thenReturn(Flux.just(new OrderItem(1L, 1L, 2, 10_000)));
        when(itemService.findById(anyLong())).thenReturn(Mono.just(new Item(1L, "Товар", "Описание товара", 10_000, false)));

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .get()
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
        verifyNoMoreInteractions(orderService, itemService);
    }

    @Test
    void buy_success() {
        when(cartService.getCartItems(anyLong())).thenReturn(Flux.just(new CartItem(1L, 1L, 3), new CartItem(1L, 2L, 2)));
        //noinspection unchecked
        when(itemService.findById(anyLong())).thenReturn(
                Mono.just(new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, false)),
                Mono.just(new Item(2L, "Intel Core i7", "Intel Core i7", 1300, false))
        );
        when(orderService.create(anyLong())).thenReturn(Mono.just(new Order(1L, 1L, LocalDateTime.now())));
        when(userService.findById(1L)).thenReturn(Mono.just(new User(1L, "user", "password", 1L, false)));
        when(accountApi.debit(anyLong(), any())).thenReturn(Mono.just(new AccountDTO().id(1L).balance(30000L)));

        webTestClient.mutateWith(mockUser(new IdentityUserDetails(1, "user", "password")))
                .mutateWith(csrf())
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .location("/orders/1?newOrder=true");

        verify(cartService, times(1)).getCartItems(1L);
        verify(itemService, times(2)).findById(anyLong());
        verify(orderService, times(1)).create(1L);
        verify(userService, times(1)).findById(1L);
        verify(accountApi, times(1)).debit(1L, new AmountDTO().amount(950000L));
        verifyNoMoreInteractions(orderService, cartService, itemService, accountApi);
    }

}
