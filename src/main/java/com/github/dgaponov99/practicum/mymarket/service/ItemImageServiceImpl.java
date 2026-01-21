package com.github.dgaponov99.practicum.mymarket.service;

import com.github.dgaponov99.practicum.mymarket.config.ImageStoreProperties;
import com.github.dgaponov99.practicum.mymarket.exception.ItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class ItemImageServiceImpl implements ItemImageService {

    private final ImageStoreProperties imageStoreProperties;

    public Flux<DataBuffer> getImage(long itemId, DataBufferFactory dataBufferFactory) {
        return DataBufferUtils.read(getImagePath(itemId),
                dataBufferFactory,
                imageStoreProperties.getBufferChunkSize()
        ).onErrorMap(e -> new ItemNotFoundException(itemId));
    }

    protected Path getImagePath(long itemId) {
        return Paths.get(imageStoreProperties.getStoreDirectoryPath(), "%d.png".formatted(itemId));
    }

}
