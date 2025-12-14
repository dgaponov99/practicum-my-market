package com.github.dgaponov99.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ImportTestcontainers(PostgreSQLTestcontainer.class)
class PracticumMyMarketApplicationIT {

    @Test
    void contextLoads() {
    }

}
