package com.github.dgaponov99.practicum.mymarket.app;

import com.github.dgaponov99.practicum.mymarket.app.client.api.AccountApi;
import com.github.dgaponov99.practicum.mymarket.app.client.dto.AccountDTO;
import com.github.dgaponov99.practicum.mymarket.app.percistence.entity.User;
import com.github.dgaponov99.practicum.mymarket.app.percistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitUsersComponent {

    private final AccountApi accountApi;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        userRepository.count()
                .filter(userCount -> userCount == 0)
                .flatMapMany(c -> Flux.fromStream(Stream.of("user1", "user2", "user3"))
                        .flatMap(username -> accountApi.createAccount(10000L)
                                .map(AccountDTO::getId)
                                .flatMap(accountId ->
                                        userRepository.save(new User(null, username, passwordEncoder.encode(username), accountId, false)))))
                .then()
                .doOnSuccess(u -> log.info("users has been created"))
                .doOnError(ex -> log.warn("error while creating users", ex))
                .subscribe();
    }

}
