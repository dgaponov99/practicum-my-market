package com.github.dgaponov99.practicum.mymarket.payment;

import com.github.dgaponov99.practicum.mymarket.payment.config.AccountProperties;
import com.github.dgaponov99.practicum.mymarket.payment.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitAccountComponent {

    private final AccountProperties accountProperties;
    private final AccountService accountService;

    @EventListener(ApplicationReadyEvent.class)
    void init() {
        accountService.account()
                .switchIfEmpty(accountService.create(accountProperties.getInitialBalance()))
                .subscribe(account -> log.info("default account: {}", account));
    }

}
