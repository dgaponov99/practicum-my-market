package com.github.dgaponov99.practicum.mymarket.app.integration.run;

import com.github.dgaponov99.practicum.mymarket.app.PracticumMyMarketAppApplication;
import com.github.dgaponov99.practicum.mymarket.app.integration.run.component.TestDataInitConfiguration;
import com.github.dgaponov99.practicum.mymarket.app.integration.run.component.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestPracticumMyMarketAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(PracticumMyMarketAppApplication::main)
                .with(TestcontainersConfiguration.class, TestDataInitConfiguration.class)
                .run(args);
    }

}
