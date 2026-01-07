package com.github.dgaponov99.practicum.mymarket.integration.run;

import com.github.dgaponov99.practicum.mymarket.PracticumMyMarketApplication;
import com.github.dgaponov99.practicum.mymarket.integration.run.component.TestDataInitConfiguration;
import com.github.dgaponov99.practicum.mymarket.integration.run.component.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestPracticumMyMarketApplication {

    public static void main(String[] args) {
        SpringApplication.from(PracticumMyMarketApplication::main)
                .with(TestcontainersConfiguration.class, TestDataInitConfiguration.class)
                .run(args);
    }

}
