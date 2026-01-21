package com.github.dgaponov99.practicum.mymarket.service;

import com.github.dgaponov99.practicum.mymarket.percistence.ItemsSortBy;
import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.percistence.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Mono<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Flux<Item> search(String searchText, int pageNumber, int pageSize, ItemsSortBy sortBy) {
        var pageable = PageRequest.of(pageNumber, pageSize, switch (sortBy) {
            case ALPHA -> Sort.by(Sort.Direction.ASC, "title");
            case PRICE -> Sort.by(Sort.Direction.ASC, "price");
            case NO -> Sort.by(Sort.Direction.DESC, "id");
        });
        return itemRepository.search(StringUtils.hasText(searchText) ? searchText : null, pageable);
    }

    public Mono<Integer> searchCount(String searchText) {
        return itemRepository.searchCount(StringUtils.hasText(searchText) ? searchText : null);
    }

}
