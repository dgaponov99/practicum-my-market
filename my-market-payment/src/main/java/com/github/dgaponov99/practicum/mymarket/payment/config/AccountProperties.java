package com.github.dgaponov99.practicum.mymarket.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AccountProperties {

    @Value("${account.initialBalace:500000}")
    private long initialBalance;

}
