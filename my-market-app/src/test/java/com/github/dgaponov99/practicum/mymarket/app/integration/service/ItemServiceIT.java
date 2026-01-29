package com.github.dgaponov99.practicum.mymarket.app.integration.service;

import com.github.dgaponov99.practicum.mymarket.app.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.ItemRepository;
import com.github.dgaponov99.practicum.mymarket.app.service.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemServiceIT extends ServiceIT {

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ItemService itemService;

    @Test
    void findById_success() {
        var expectItem = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(expectItem)
                .then();

        setupData.then(itemService.findById(1L))
                .doOnNext(actualItem -> assertAll(
                        () -> assertEquals(expectItem.getId(), actualItem.getId()),
                        () -> assertEquals(expectItem.getTitle(), actualItem.getTitle()),
                        () -> assertEquals(expectItem.getDescription(), actualItem.getDescription()),
                        () -> assertEquals(expectItem.getPrice(), actualItem.getPrice())
                ))
                .block();
    }

    @Test
    void findById_empty() {
        var expectItem = new Item(1L, "Товар", "Описание товара", 10_000, true);
        var setupData = itemRepository.save(expectItem)
                .then();

        setupData.then(itemService.findById(100500L))
                .doOnNext(actualItem -> assertThat(actualItem).isNull())
                .block();
    }

    @ParameterizedTest
    @CsvSource({
            ", 0, 10, NO, 8, 8",
            ", 2, 2, NO, 2, 8",
            "i7, 1, 2, ALPHA, 2, 4",
            "Xeon, 0, 10, ALPHA, 2, 2",
            "Processor, 0, 10, PRICE, 1, 1",
    })
    void search_success(String search, int pageNumber, int pageSize, ItemsSortBy sortBy, int expectedSize, int expectedTotaElements) {
        itemRepository.saveAll(List.of(
                        new Item(1L, "Intel Core i7", "Intel Core i7 4th gen", 2300, true),
                        new Item(2L, "Intel Core i7", "Intel Core i7", 1300, true),
                        new Item(3L, "Intel Core i3", "Intel Core i3", 1900, true),
                        new Item(4L, "Intel Xeon", "Intel Xeon Processor", 3000, true),
                        new Item(5L, "Intel Xeon", "Intel Xeon E5-2600", 2300, true),
                        new Item(6L, "Intel Core i7", "Intel Core i7 X-series", 2300, true),
                        new Item(7L, "Intel Core i7", "Intel Core i7 9th gen", 2300, true),
                        new Item(8L, "Intel Core 2 Duo", "Intel Core 2 Duo", 2300, true)
                ))
                .then().block();

        itemService.searchCount(search)
                .doOnNext(actualCount -> assertThat(actualCount)
                        .isEqualTo(expectedTotaElements))
                .block();
        itemService.search(search, pageNumber, pageSize, sortBy)
                .collectList()
                .doOnNext(searchContent -> assertThat(searchContent)
                        .hasSize(expectedSize)
                )
                .block();
    }

}
