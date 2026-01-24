package com.github.dgaponov99.practicum.mymarket.app.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ImageStoreProperties {

    @Value("${item.image.directory:images}")
    private String storeDirectoryPath;
    @Value("${item.image.buffer.size:4096}")
    private int bufferChunkSize;

}
