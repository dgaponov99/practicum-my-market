package com.github.dgaponov99.practicum.mymarket.app.integration;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;

public class RedisTestcontainer {

    @Container
    @ServiceConnection
    public static final RedisContainer redis = new RedisContainer("redis:7.4.2-bookworm");

}
