package com.github.dgaponov99.practicum.mymarket.app.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import reactor.core.publisher.Flux;

public interface ItemImageService {

    Flux<DataBuffer> getImage(long itemId, DataBufferFactory dataBufferFactory);

}
