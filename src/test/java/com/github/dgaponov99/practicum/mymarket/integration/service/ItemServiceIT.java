package com.github.dgaponov99.practicum.mymarket.integration.service;

import com.github.dgaponov99.practicum.mymarket.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import com.github.dgaponov99.practicum.mymarket.service.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ItemServiceIT extends ServiceIT {

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ItemService itemService;

    @Test
    void findById_success() {
        var expectItem = new Item(1L, "Товар", "Описание товара", 10_000);
        itemRepository.saveAndFlush(expectItem);

        var actualItem = itemService.findById(1L).orElseThrow();

        assertAll(() -> {
            assertEquals(expectItem, actualItem);
            assertEquals(expectItem.getId(), actualItem.getId());
            assertEquals(expectItem.getTitle(), actualItem.getTitle());
            assertEquals(expectItem.getDescription(), actualItem.getDescription());
            assertEquals(expectItem.getPrice(), actualItem.getPrice());
        });
    }

    @Test
    void findById_empty() {
        var expectItem = new Item(1L, "Товар", "Описание товара", 10_000);
        itemRepository.saveAndFlush(expectItem);

        var actualItemOpt = itemService.findById(100500L);

        assertTrue(actualItemOpt.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            ", 0, 10, NO, 8, 8",
            ", 2, 2, NO, 2, 8",
            "i7, 1, 2, ALPHA, 2, 4",
            "xeon, 0, 10, ALPHA, 2, 2",
            "processor, 0, 10, PRICE, 1, 1",
    })
    void search_success(String search, int pageNumber, int pageSize, ItemsSortBy sortBy, int expectedSize, int expectedTotaElements) {
        itemRepository.saveAll(List.of(
                new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300),
                new Item(2L, "Intel Core i7", "Intel Core i7", 1300),
                new Item(3L, "Intel Core i3", "Intel Core i3", 1900),
                new Item(4L, "Intel Xeon", "Intel Xeon Processor", 3000),
                new Item(5L, "Intel Xeon", "Intel Xeon E5-2600", 2300),
                new Item(6L, "Intel Core i7", "Intel Core i7 X-series", 2300),
                new Item(7L, "Intel Core i7", "Intel Core i7 9th gen", 2300),
                new Item(8L, "Intel Core 2 Duo", "Intel Core 2 Duo", 2300)
        ));

        var itemPage = itemService.search(search, pageNumber, pageSize, sortBy);
        assertAll(() -> {
            assertEquals(expectedSize, itemPage.getContent().size());
            assertEquals(expectedTotaElements, itemPage.getTotalElements());
        });
    }

}
