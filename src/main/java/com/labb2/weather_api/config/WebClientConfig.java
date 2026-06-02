package com.labb2.weather_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Här skapar jag en WebClient.Builder som en Spring Bean
 * Både WeatherClient och ActivityClient använder denna för att göra sina HTTP-anrop
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
