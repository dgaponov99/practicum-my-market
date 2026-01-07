package com.github.dgaponov99.practicum.mymarket.integration.run.component;

import com.github.dgaponov99.practicum.mymarket.exception.ImageItemNotFoundException;
import com.github.dgaponov99.practicum.mymarket.service.ItemImageService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Primary
public class TestImageService implements ItemImageService {

    @Override
    public InputStream getImage(long itemId) {
        var imageIs = getClass().getClassLoader().getResourceAsStream("images/%d.png".formatted(itemId));
        if (imageIs == null) {
            throw new ImageItemNotFoundException(itemId);
        }
        return imageIs;
    }

}
