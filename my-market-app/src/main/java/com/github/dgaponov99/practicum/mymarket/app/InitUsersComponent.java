package com.github.dgaponov99.practicum.mymarket.app;

import com.github.dgaponov99.practicum.mymarket.app.client.api.AccountApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitUsersComponent {

    private final AccountApi accountApi;

    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        Optional.ofNullable(accountApi.getAccountWithHttpInfo(1L))
                .ifPresent(responseMono -> responseMono
                        .filter(responseEntity -> responseEntity.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(200)))
                        .map(HttpEntity::getBody)
                        .switchIfEmpty(accountApi.createAccount(10000L)
                                .doOnNext(account -> log.info("Created default account with id: {}", account.getId())))
                        .doOnError(ex -> log.warn("Error while creating default account", ex))
                        .subscribe(account -> log.info("Usage default account with id: {}", account.getId())));
    }

}
