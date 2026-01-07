package com.github.dgaponov99.practicum.mymarket.service;

import com.github.dgaponov99.practicum.mymarket.exception.ItemNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ItemImageServiceImpl implements ItemImageService {

    @Value("${item.image.directory:images}")
    private String itemImageDirectoryPath;

    public InputStream getImage(long itemId) {
        if (Files.notExists(getImagePath(itemId))) {
            throw new ItemNotFoundException(itemId);
        }
        try {
            return Files.newInputStream(getImagePath(itemId));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected Path getImagePath(long itemId) {
        return Paths.get(itemImageDirectoryPath, "%d.png".formatted(itemId));
    }

}
