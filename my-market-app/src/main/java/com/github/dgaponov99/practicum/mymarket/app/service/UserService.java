package com.github.dgaponov99.practicum.mymarket.app.service;

import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.User;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Mono<User> findById(long id) {
        return userRepository.findById(id);
    }

}
