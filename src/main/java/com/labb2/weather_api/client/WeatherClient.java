package com.labb2.weather_api.client;

import com.labb2.weather_api.dto.WeatherData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

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
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;
    // Anropar en metod utifrån
    public Mono<WeatherData> getWeather(String city) {
        ReactiveCircuitBreaker cb = circuitBreakerFactory.create("weatherApi");
        return cb.run(
                webClientBuilder
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
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .map(response -> new WeatherData(
                        response.location().name(),
                        response.current().condition().text(),
                        response.current().temp_c()
                )),
                throwable -> Mono.just(new WeatherData(
                        "Unknown", "Service unavailable", 0.0))
        );
    }
    // Private records för att tolka WeatherAPI:s JSON svar!
    private record WeatherApiResponse(Location location, Current current) {}
    private record Location(String name) {}
    private record Current(double temp_c, Condition condition) {}
    private record Condition(String text) {}
}
