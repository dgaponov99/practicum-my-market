package com.github.dgaponov99.practicum.mymarket.app.percistence.repository.impl;

import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.Item;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.ItemRepositoryFluent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Repository
@RequiredArgsConstructor
public class ItemRepositoryFluentImpl implements ItemRepositoryFluent {

    private final R2dbcEntityTemplate template;

    @Override
    public Flux<Item> search(String searchText, Pageable pageable) {
        return template.select(searchQuery(searchText).with(pageable), Item.class);
    }

    @Override
    public Mono<Integer> searchCount(String searchText) {
        return template.count(searchQuery(searchText), Item.class).map(Long::intValue);
    }

    private Query searchQuery(String searchText) {
        var criteriaList = new ArrayList<Criteria>();

        if (StringUtils.hasText(searchText)) {
            criteriaList.add(Criteria.where("title").like("%" + searchText + "%")
                    .or("description").like("%" + searchText + "%"));
        }

        return criteriaList.isEmpty() ? Query.empty() : Query.query(CriteriaDefinition.from(criteriaList));
    }

}
