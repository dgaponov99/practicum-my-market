package com.github.dgaponov99.practicum.mymarket.app.web.controller;

import com.github.dgaponov99.practicum.mymarket.app.config.MarketViewProperties;
import com.github.dgaponov99.practicum.mymarket.app.exception.ImageItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.app.web.CartAction;
import com.github.dgaponov99.practicum.mymarket.app.web.service.MarketViewService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class MarketController {

    private final MarketViewService marketViewService;
    private final MarketViewProperties marketViewProperties;

    @GetMapping({"/", "/items"})
    public Mono<Rendering> search(@RequestParam(required = false) String search,
                                  @RequestParam(defaultValue = "NO") ItemsSortBy sort,
                                  @RequestParam(defaultValue = "1") int pageNumber,
                                  @RequestParam(defaultValue = "5") int pageSize) {
        return marketViewService.search(search, pageNumber, pageSize, sort)
                .map(itemsView -> Rendering.view("items")
                        .modelAttribute("items", ListUtils.partition(itemsView.getItems(), marketViewProperties.getItemsPartitionSize()))
                        .modelAttribute("paging", itemsView.getPaging())
                        .modelAttribute("search", search)
                        .modelAttribute("sort", sort)
                        .build()
                );
    }

    @PostMapping("/items")
    public Mono<Rendering> itemCartAction(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> marketViewService.cartAction(
                                Long.parseLong(formData.getFirst("id")),
                                CartAction.valueOf(formData.getFirst("action"))
                        )
                        .thenReturn(redirect("/items", exchange.getRequest().getQueryParams(), formData, "id", "action")));
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> item(@PathVariable long id) {
        return marketViewService.getItem(id)
                .map(item -> Rendering.view("item")
                        .modelAttribute("item", item)
                        .build()
                );
    }

    @PostMapping("/items/{id}")
    public Mono<String> item(@PathVariable long id,
                             ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> marketViewService.cartAction(id,
                                CartAction.valueOf(formData.getFirst("action")))
                        .thenReturn("redirect:/items/%d".formatted(id)));
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> cart() {
        return marketViewService.getCartItems()
                .collectList()
                .flatMap(items -> {
                    var total = marketViewService.calculateTotalPrice(items);
                    return marketViewService.enableBuy(total)
                            .map(enableBuyView ->
                                    Rendering.view("cart")
                                            .modelAttribute("items", items)
                                            .modelAttribute("total", total)
                                            .modelAttribute("enableBuy", enableBuyView)
                                            .build());
                });
    }

    @PostMapping("/cart/items")
    public Mono<String> cartAction(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> marketViewService.cartAction(
                                Long.parseLong(formData.getFirst("id")),
                                CartAction.valueOf(formData.getFirst("action"))
                        )
                        .thenReturn("redirect:/cart/items"));
    }

    @GetMapping("/orders")
    public Mono<Rendering> orders() {
        return Mono.just(Rendering.view("orders")
                .modelAttribute("orders", marketViewService.getOrders())
                .build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> order(@PathVariable long id,
                                 @RequestParam(defaultValue = "false") boolean newOrder) {
        return marketViewService.getOrder(id)
                .map(order -> Rendering.view("order")
                        .modelAttribute("order", order)
                        .modelAttribute("newOrder", newOrder)
                        .build()
                );
    }

    @PostMapping("/buy")
    public Mono<String> buy() {
        return marketViewService.buy().map("redirect:/orders/%d?newOrder=true"::formatted);
    }

    @GetMapping(value = "/images/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> image(@PathVariable long id, ServerHttpResponse response) {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(marketViewService.getItemImageResource(id, response.bufferFactory())
                                .onErrorResume(ImageItemNotFoundException.class,
                                        e -> Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND))
                                ))
        );
    }

    private Rendering redirect(String redirectUrl, MultiValueMap<String, String> queryParams, MultiValueMap<String, String> formData, String... excludeParams) {
        var excludeParamsSet = Set.of(excludeParams);

        var uriBuilder = UriComponentsBuilder.fromUriString(redirectUrl);
        queryParams.forEach((key, value) -> {
            if (!excludeParamsSet.contains(key)) {
                uriBuilder.queryParam(key, value);
            }
        });
        formData.forEach((key, value) -> {
            if (!excludeParamsSet.contains(key)) {
                uriBuilder.queryParam(key, value);
            }
        });

        return Rendering.redirectTo(uriBuilder.toUriString()).build();
    }

}
