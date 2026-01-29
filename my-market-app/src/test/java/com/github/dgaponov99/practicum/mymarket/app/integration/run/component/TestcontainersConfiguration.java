package com.github.dgaponov99.practicum.mymarket.app.integration.run.component;

import com.github.dgaponov99.practicum.mymarket.app.integration.PostgreSQLTestcontainer;
import com.github.dgaponov99.practicum.mymarket.app.integration.RedisTestcontainer;
import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return PostgreSQLTestcontainer.postgres;
    }

    @Bean
    @ServiceConnection
    RedisContainer redisContainer() {
        return RedisTestcontainer.redis;
    }

}
