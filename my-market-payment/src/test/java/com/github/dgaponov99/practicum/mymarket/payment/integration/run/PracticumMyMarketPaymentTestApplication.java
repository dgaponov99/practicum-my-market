package com.github.dgaponov99.practicum.mymarket.payment.integration.run;

import com.github.dgaponov99.practicum.mymarket.payment.PracticumMyMarketPaymentApplication;
import org.springframework.boot.SpringApplication;

public class PracticumMyMarketPaymentTestApplication {

    public static void main(String[] args) {
        SpringApplication.from(PracticumMyMarketPaymentApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
