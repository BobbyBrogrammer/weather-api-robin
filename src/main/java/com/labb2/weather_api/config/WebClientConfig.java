package com.labb2.weather_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Konfigurationsklass som registrerar WebClient.Builder som en Spring Bean
 * Som används av WeatherClient och ActivityClient för att bygga sina egna WebClient instanser
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
