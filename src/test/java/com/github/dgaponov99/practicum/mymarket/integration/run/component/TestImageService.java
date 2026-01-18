package com.github.dgaponov99.practicum.mymarket.integration.run.component;

import com.github.dgaponov99.practicum.mymarket.exception.ItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.service.ItemImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Primary
public class TestImageService implements ItemImageService {

    @Value("${item.image.buffer.size:4096}")
    private int bufferChunkSize;

    public Flux<DataBuffer> getImage(long itemId, DataBufferFactory dataBufferFactory) {
        return DataBufferUtils.read(new ClassPathResource("images/%d.png".formatted(itemId)),
                dataBufferFactory,
                bufferChunkSize
        ).onErrorMap(e -> new ItemNotFoundException(itemId));
    }
}
