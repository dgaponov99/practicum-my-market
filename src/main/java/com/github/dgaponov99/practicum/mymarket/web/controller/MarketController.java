package com.github.dgaponov99.practicum.mymarket.web.controller;

import com.github.dgaponov99.practicum.mymarket.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.web.CartAction;
import com.github.dgaponov99.practicum.mymarket.web.service.MarketViewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class MarketController {

    private final MarketViewService marketViewService;

    @Value("${items.partition.size:3}")
    private int itemsPartitionSize;

    @GetMapping({"/", "/items"})
    public String search(@RequestParam(required = false) String search,
                         @RequestParam(defaultValue = "NO") ItemsSortBy sort,
                         @RequestParam(defaultValue = "1") int pageNumber,
                         @RequestParam(defaultValue = "5") int pageSize,
                         Model model) {
        var itemsView = marketViewService.search(search, pageNumber, pageSize, sort);
        model.addAttribute("items", ListUtils.partition(itemsView.getItems(), itemsPartitionSize));
        model.addAttribute("paging", itemsView.getPaging());
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        return "items";
    }

    @PostMapping("/items")
    public String itemCartAction(@RequestParam long id,
                                 @RequestParam CartAction action,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        marketViewService.cartAction(id, action);
        return redirect("/items", request, redirectAttributes, "id", "action");
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable long id, Model model) {
        var item = marketViewService.getItem(id);
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/items/{id}")
    public String item(@PathVariable long id,
                       @RequestParam CartAction action,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {
        marketViewService.cartAction(id, action);
        redirectAttributes.addAttribute("id", id);
        return redirect("/items/{id}", request, redirectAttributes, "action");
    }

    @GetMapping("/cart/items")
    public String cart(Model model) {
        var items = marketViewService.getCartItems();
        var total = marketViewService.calculateTotalPrice(items);
        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/cart/items")
    public String cartAction(@RequestParam long id,
                             @RequestParam CartAction action,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        marketViewService.cartAction(id, action);
        return redirect("/cart/items", request, redirectAttributes, "id", "action");
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        var orders = marketViewService.getOrders();
        model.addAttribute("orders", orders);

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable long id,
                        @RequestParam(defaultValue = "false") boolean newOrder,
                        Model model) {
        var order = marketViewService.getOrder(id);
        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);

        return "order";
    }

    @PostMapping("/buy")
    public String buy(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        var createdOrderId = marketViewService.buy();
        redirectAttributes.addAttribute("id", createdOrderId);
        return redirect("/orders/{id}?newOrder=true", request, redirectAttributes);
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> image(@PathVariable long id) {
        return ResponseEntity.of(marketViewService.getItemImageResource(id));
    }

    private String redirect(String redirectUrl, HttpServletRequest request, RedirectAttributes redirectAttributes, String... excludeParams) {
        request.getParameterMap().forEach((key, value) -> {
            if (!Set.of(excludeParams).contains(key)) {
                redirectAttributes.addAttribute(key, value);
            }
        });
        return "redirect:%s".formatted(redirectUrl);
    }

}
