package com.github.dgaponov99.practicum.mymarket.percistence.repository;

import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemRepositoryFluent {

    Flux<Item> search(String searchText, Pageable pageable);

    Mono<Integer> searchCount(String searchText);

}
