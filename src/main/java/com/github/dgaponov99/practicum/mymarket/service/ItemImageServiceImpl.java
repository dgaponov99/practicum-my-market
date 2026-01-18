package com.github.dgaponov99.practicum.mymarket.service;

import com.github.dgaponov99.practicum.mymarket.exception.ItemNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ItemImageServiceImpl implements ItemImageService {

    @Value("${item.image.directory:images}")
    private String storeDirectoryPath;
    @Value("${item.image.buffer.size:4096}")
    private int bufferChunkSize;

    public Flux<DataBuffer> getImage(long itemId, DataBufferFactory dataBufferFactory) {
        return DataBufferUtils.read(getImagePath(itemId),
                dataBufferFactory,
                bufferChunkSize
        ).onErrorMap(e -> new ItemNotFoundException(itemId));
    }

    protected Path getImagePath(long itemId) {
        return Paths.get(storeDirectoryPath, "%d.png".formatted(itemId));
    }

}
