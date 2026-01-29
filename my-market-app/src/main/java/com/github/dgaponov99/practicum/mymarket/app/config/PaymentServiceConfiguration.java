package com.github.dgaponov99.practicum.mymarket.app.config;

import com.github.dgaponov99.practicum.mymarket.app.client.api.AccountApi;
import com.github.dgaponov99.practicum.mymarket.app.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentServiceConfiguration {

    @Value("${payment.service.url:http://localhost:8081}")
    private String url;

    @Bean
    AccountApi accountApi(WebClient.Builder webClientBuilder) {
        return new AccountApi(new ApiClient(webClientBuilder.build()).setBasePath(url));
    }

}
