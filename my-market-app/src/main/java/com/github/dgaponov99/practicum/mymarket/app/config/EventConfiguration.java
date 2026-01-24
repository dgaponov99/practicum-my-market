package com.github.dgaponov99.practicum.mymarket.app.config;

import com.github.dgaponov99.practicum.mymarket.app.event.DomainEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

@Configuration
public class EventConfiguration {

    @Bean
    public Sinks.Many<Object> eventBusSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public DomainEventBus domainEventBus(Sinks.Many<Object> eventBusSink) {
        return new DomainEventBus(eventBusSink);
    }

}
