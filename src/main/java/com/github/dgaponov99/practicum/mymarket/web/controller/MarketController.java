package com.github.dgaponov99.practicum.mymarket.web.controller;

import com.github.dgaponov99.practicum.mymarket.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.web.CartAction;
import com.github.dgaponov99.practicum.mymarket.web.adapter.ItemAdapter;
import com.github.dgaponov99.practicum.mymarket.web.adapter.OrderAdapter;
import com.github.dgaponov99.practicum.mymarket.web.adapter.PagingAdapter;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.ArrayList;
import java.util.Set;

@Controller
public class MarketController {

    @Value("${items.partition.size:3}")
    private int itemsPartitionSize;

    @GetMapping({"/", "/items"})
    public String search(@RequestParam(required = false) String search,
                         @RequestParam(defaultValue = "NO") ItemsSortBy sort,
                         @RequestParam(defaultValue = "1") int pageNumber,
                         @RequestParam(defaultValue = "5") int pageSize,
                         Model model) {

        var items = new ArrayList<ItemAdapter>();
        var paging = new PagingAdapter(null);
        model.addAttribute("items", ListUtils.partition(items, itemsPartitionSize));
        model.addAttribute("paging", paging);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        return "items";
    }

    @PostMapping("/items")
    public String itemCartAction(@RequestParam long id,
                                 @RequestParam CartAction action,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {

        return redirect("/items", request, redirectAttributes, "id", "actions");
    }

    @GetMapping
    public String item(@PathVariable long id, Model model) {
        var item = new ItemAdapter();
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/items/{id}")
    public String item(@PathVariable long id,
                       @RequestParam CartAction action,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("id", id);
        return redirect("/items/{id}", request, redirectAttributes, "action");
    }

    @GetMapping("/cart/items")
    public String cart(Model model) {

        var items = new ArrayList<ItemAdapter>();
        var total = items.stream().mapToLong(item -> item.getPrice() * item.getCount()).sum();
        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/cart/items")
    public String cartAction(@RequestParam long id,
                             @RequestParam CartAction action,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        return redirect("/items", request, redirectAttributes, "id", "action");
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        var orders = new ArrayList<OrderAdapter>();
        model.addAttribute("orders", orders);

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable long id,
                        @RequestParam(defaultValue = "false") boolean newOrder,
                        Model model) {
        var order = new OrderAdapter();
        model.addAttribute("order", order);

        return "order";
    }

    @PostMapping("/buy")
    public String buy(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        var createdOrderId = 0L;
        redirectAttributes.addAttribute("id", createdOrderId);
        return redirect("/orders/{id}?newOrder=true", request, redirectAttributes);
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> image(@PathVariable long id) {
        return ResponseEntity.noContent().build();
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
