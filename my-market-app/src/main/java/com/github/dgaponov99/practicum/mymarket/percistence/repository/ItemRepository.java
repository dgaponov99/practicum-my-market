package com.github.dgaponov99.practicum.mymarket.percistence.repository;

import com.github.dgaponov99.practicum.mymarket.percistence.entity.Item;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends ReactiveCrudRepository<Item, Long>, ItemRepositoryFluent {

}
