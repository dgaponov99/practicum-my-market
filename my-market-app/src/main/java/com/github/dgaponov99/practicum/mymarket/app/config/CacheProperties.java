package com.github.dgaponov99.practicum.mymarket.app.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class CacheProperties {

    @Value("${cache.redis.seconds:120}")
    private long cacheRedisSeconds;

    @Value("${cache.http.image.seconds:120}")
    private long cacheHttpImageSeconds;

}
