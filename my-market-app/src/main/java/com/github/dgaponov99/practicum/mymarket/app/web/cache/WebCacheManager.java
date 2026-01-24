package com.github.dgaponov99.practicum.mymarket.app.web.cache;

import com.github.dgaponov99.practicum.mymarket.app.event.CartItemChangeEvent;
import com.github.dgaponov99.practicum.mymarket.app.event.DomainEventBus;
import com.github.dgaponov99.practicum.mymarket.app.event.ItemChangeEvent;
import com.github.dgaponov99.practicum.mymarket.app.event.OrderChangeEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebCacheManager {

    private final CacheManager cacheManager;
    private final DomainEventBus domainEventBus;

    @PostConstruct
    public void init() {
        domainEventBus.listen(ItemChangeEvent.class)
                .subscribe(event -> {
                            log.debug("new item event: {}", event);
                            Optional.ofNullable(cacheManager.getCache("itemView")).ifPresent(cache -> {
                                if (event.id() > 0) cache.evict(event.id());
                                else cache.clear();
                            });
                            Optional.ofNullable(cacheManager.getCache("itemsPageView")).ifPresent(Cache::clear);
                        }
                );

        domainEventBus.listen(CartItemChangeEvent.class)
                .subscribe(event -> {
                            log.debug("new cart item event: {}", event);
                            Optional.ofNullable(cacheManager.getCache("itemView")).ifPresent(cache -> {
                                if (event.itemId() > 0) cache.evict(event.itemId());
                                else cache.clear();
                            });
                            Optional.ofNullable(cacheManager.getCache("itemsPageView")).ifPresent(Cache::clear);
                        }
                );

        domainEventBus.listen(OrderChangeEvent.class)
                .subscribe(event -> {
                            log.debug("new order event: {}", event);
                            Optional.ofNullable(cacheManager.getCache("orderView")).ifPresent(cache -> {
                                if (event.id() > 0) cache.evict(event.id());
                                else cache.clear();
                            });
                            Optional.ofNullable(cacheManager.getCache("ordersView")).ifPresent(Cache::clear);
                        }
                );
    }

}
