package com.github.dgaponov99.practicum.mymarket.app.integration.run.component;

import com.github.dgaponov99.practicum.mymarket.app.config.ImageStoreProperties;
import com.github.dgaponov99.practicum.mymarket.app.exception.ItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.app.service.ItemImageService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ImageStoreProperties imageStoreProperties;

    public Flux<DataBuffer> getImage(long itemId, DataBufferFactory dataBufferFactory) {
        return DataBufferUtils.read(new ClassPathResource("images/%d.png".formatted(itemId)),
                dataBufferFactory,
                imageStoreProperties.getBufferChunkSize()
        ).onErrorMap(e -> new ItemNotFoundException(itemId));
    }
}
