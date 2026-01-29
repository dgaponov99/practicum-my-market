package com.github.dgaponov99.practicum.mymarket.app.web.cache;

import com.github.dgaponov99.practicum.mymarket.app.event.CartItemChangeEvent;
import com.github.dgaponov99.practicum.mymarket.app.event.DomainEventBus;
import com.github.dgaponov99.practicum.mymarket.app.event.ItemChangeEvent;
import com.github.dgaponov99.practicum.mymarket.app.event.OrderChangeEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebCacheManager {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final DomainEventBus domainEventBus;

    @PostConstruct
    public void init() {
        domainEventBus.listen(ItemChangeEvent.class)
                .subscribe(event -> {
                            log.info("new item event: {}", event);
                            redisTemplate.scan(ScanOptions.scanOptions().match("itemsPageView:*").build())
                                    .flatMap(redisTemplate::delete)
                                    .then(Mono.defer(() -> {
                                        if (event.id() > 0) {
                                            return redisTemplate.delete("itemView:%d".formatted(event.id()));
                                        } else {
                                            return redisTemplate.scan(ScanOptions.scanOptions().match("itemView:*").build())
                                                    .flatMap(redisTemplate::delete).then();
                                        }
                                    }))
                                    .subscribe();
                        }
                );

        domainEventBus.listen(CartItemChangeEvent.class)
                .subscribe(event -> {
                            log.debug("new cart item event: {}", event);
                            redisTemplate.scan(ScanOptions.scanOptions().match("itemsPageView:*").build())
                                    .flatMap(redisTemplate::delete)
                                    .then(Mono.defer(() -> {
                                        if (event.itemId() > 0) {
                                            return redisTemplate.delete("itemView:%d".formatted(event.itemId()));
                                        } else {
                                            return redisTemplate.scan(ScanOptions.scanOptions().match("itemView:*").build())
                                                    .flatMap(redisTemplate::delete).then();
                                        }
                                    }))
                                    .subscribe();
                        }
                );

        domainEventBus.listen(OrderChangeEvent.class)
                .subscribe(event -> {
                            log.debug("new order event: {}", event);
                            redisTemplate.scan(ScanOptions.scanOptions().match("ordersView:*").build())
                                    .flatMap(redisTemplate::delete)
                                    .then(Mono.defer(() -> {
                                        if (event.id() > 0) {
                                            return redisTemplate.delete("orderView:%d".formatted(event.id()));
                                        } else {
                                            return redisTemplate.scan(ScanOptions.scanOptions().match("orderView:*").build())
                                                    .flatMap(redisTemplate::delete).then();
                                        }
                                    }))
                                    .subscribe();
                        }
                );
    }

}
