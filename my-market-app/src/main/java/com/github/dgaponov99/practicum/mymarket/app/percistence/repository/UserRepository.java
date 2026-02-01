package com.github.dgaponov99.practicum.mymarket.app.percistence.repository;

import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    Mono<User> findByUsername(String username);

}
