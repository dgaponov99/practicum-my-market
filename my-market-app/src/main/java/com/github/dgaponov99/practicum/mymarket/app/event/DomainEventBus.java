package com.github.dgaponov99.practicum.mymarket.app.event;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RequiredArgsConstructor
public class DomainEventBus {

    private final Sinks.Many<Object> sink;

    public void publish(Object event) {
        sink.tryEmitNext(event);
    }

    public <T> Flux<T> listen(Class<T> type) {
        return sink.asFlux().ofType(type);
    }

}
