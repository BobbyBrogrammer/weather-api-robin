package com.labb2.weather_api.client;

import com.labb2.weather_api.dto.WeatherData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Den här klassen representerar en klient för att hämta väderdata från WeatherAPI,
 * Autentiseringen sker via en API-nyckel som Query parameter i URL:en
 */
@Component
@RequiredArgsConstructor
public class WeatherClient {

    // Hämtar dessa från application.properties med hjälp av @Value
    @Value("${weather.api.url}")
    private String baseUrl;

    @Value("${weather.api.key}")
    private String apiKey;

    private final WebClient.Builder webClientBuilder;
    // Anropar en metod utifrån
    public Mono<WeatherData> getWeather(String city) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/current.json")
                        .queryParam("key", apiKey)
                        .queryParam("q", city)
                        .build())
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .map(response -> new WeatherData(
                        response.location().name(),
                        response.current().condition().text(),
                        response.current().temp_c()
                ));
    }
    // Private records för att tolka WeatherAPI:s JSON svar!
    private record WeatherApiResponse(Location location, Current current) {}
    private record Location(String name) {}
    private record Current(double temp_c, Condition condition) {}
    private record Condition(String text) {}
}
