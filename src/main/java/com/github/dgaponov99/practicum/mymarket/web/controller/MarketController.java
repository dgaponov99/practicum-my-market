package com.github.dgaponov99.practicum.mymarket.web.controller;

import com.github.dgaponov99.practicum.mymarket.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.web.CartAction;
import com.github.dgaponov99.practicum.mymarket.web.service.MarketViewService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class MarketController {

    private final MarketViewService marketViewService;

    @Value("${items.partition.size:3}")
    private int itemsPartitionSize;

    @GetMapping({"/", "/items"})
    public Mono<Rendering> search(@RequestParam(required = false) String search,
                                  @RequestParam(defaultValue = "NO") ItemsSortBy sort,
                                  @RequestParam(defaultValue = "1") int pageNumber,
                                  @RequestParam(defaultValue = "5") int pageSize) {
        var itemsView = marketViewService.search(search, pageNumber, pageSize, sort);
        return Mono.just(Rendering.view("items")
                .modelAttribute("items", ListUtils.partition(itemsView.getItems(), itemsPartitionSize))
                .modelAttribute("paging", itemsView.getPaging())
                .modelAttribute("search", search)
                .modelAttribute("sort", sort)
                .build());
    }

    @PostMapping("/items")
    public Mono<Rendering> itemCartAction(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    marketViewService.cartAction(Long.parseLong(formData.getFirst("id")), CartAction.valueOf(formData.getFirst("action")));
                    return Mono.just(redirect("/items", exchange.getRequest().getQueryParams(), formData, "id", "action"));
                });
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> item(@PathVariable long id) {
        var item = marketViewService.getItem(id);
        return Mono.just(Rendering.view("item")
                .modelAttribute("item", item)
                .build());
    }

    @PostMapping("/items/{id}")
    public Mono<String> item(@PathVariable long id,
                             ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(formData -> {
            marketViewService.cartAction(id, CartAction.valueOf(formData.getFirst("action")));
            return Mono.just("redirect:/items/%d".formatted(id));
        });
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> cart() {
        var items = marketViewService.getCartItems();
        var total = marketViewService.calculateTotalPrice(items);

        return Mono.just(Rendering.view("cart")
                .modelAttribute("items", items)
                .modelAttribute("total", total)
                .build());
    }

    @PostMapping("/cart/items")
    public Mono<String> cartAction(ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(formData -> {
            marketViewService.cartAction(Long.parseLong(formData.getFirst("id")), CartAction.valueOf(formData.getFirst("action")));
            return Mono.just("redirect:/cart/items");
        });
    }

    @GetMapping("/orders")
    public Mono<Rendering> orders() {
        var orders = marketViewService.getOrders();

        return Mono.just(Rendering.view("orders")
                .modelAttribute("orders", orders)
                .build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> order(@PathVariable long id,
                                 @RequestParam(defaultValue = "false") boolean newOrder) {
        var order = marketViewService.getOrder(id);

        return Mono.just(Rendering.view("order")
                .modelAttribute("order", order)
                .modelAttribute("newOrder", newOrder)
                .build());
    }

    @PostMapping("/buy")
    public Mono<String> buy(ServerWebExchange exchange) {
        var createdOrderId = marketViewService.buy();
        return Mono.just("redirect:/orders/%d?newOrder=true".formatted(createdOrderId));
    }

    @GetMapping("/images/{id}")
    public Mono<ResponseEntity<Resource>> image(@PathVariable long id) {
        return Mono.just(ResponseEntity.of(marketViewService.getItemImageResource(id)));
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
