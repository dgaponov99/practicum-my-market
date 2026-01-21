package com.github.dgaponov99.practicum.mymarket.integration.run.component;

import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.util.List;

@Slf4j
@TestConfiguration
@RequiredArgsConstructor
public class TestDataInitConfiguration {

    private final ItemRepository itemRepository;
    private final TransactionalOperator tx;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Init test data");
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
                .as(tx::transactional)
                .subscribe();
    }
}
