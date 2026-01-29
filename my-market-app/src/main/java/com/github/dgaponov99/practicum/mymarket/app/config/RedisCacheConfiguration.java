package com.github.dgaponov99.practicum.mymarket.app.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisCacheConfiguration {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory,
                                                                       ObjectMapper objectMapper) {
        var redisMapper = objectMapper.copy()
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );

        var context = RedisSerializationContext.<String, Object>newSerializationContext()
                .hashKey(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .hashValue(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .key(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .value(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(redisMapper)))
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

}
