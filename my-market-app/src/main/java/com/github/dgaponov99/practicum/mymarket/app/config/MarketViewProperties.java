package com.github.dgaponov99.practicum.mymarket.app.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class MarketViewProperties {

    @Value("${items.partition.size:3}")
    private int itemsPartitionSize;

}
