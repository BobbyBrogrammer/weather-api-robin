package com.labb2.weather_api.client;

import com.labb2.weather_api.dto.ActivityDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Den här klassen representerar en klient för att hämta aktivitetsrekommendationer från Geoapify
 * och autentiseringen sker via API-nyckel som Bearer token i Authorization-headern
 * och sök kategorin anpassas baserat på aktuellt väderförhållande
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityClient {

    private static final Map<String, String> CITY_FILTERS = Map.of(
            "stockholm", "circle:18.0686,59.3293,5000",
            "gothenburg", "circle:11.9746,57.7089,5000",
            "göteborg", "circle:11.9746,57.7089,5000",
            "goteborg", "circle:11.9746,57.7089,5000",
            "malmö", "circle:13.0038,55.6050,5000",
            "malmo", "circle:13.0038,55.6050,5000",
            "kungälv", "circle:11.9766,57.8705,5000",
            "kungalv", "circle:11.9766,57.8705,5000"
    );

    @Value("${activity.api.url}")
    private String baseUrl;

    @Value("${activity.api.key}")
    private String apiKey;

    private final WebClient.Builder webClientBuilder;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<List<ActivityDto>> getActivities(String city, String weatherCondition) {
        ReactiveCircuitBreaker cb = circuitBreakerFactory.create("activityApi");
        String category = mapWeatherToCategory(weatherCondition);
        log.info("Fetching activities — city: {}, condition: {}, category: {}", city, weatherCondition, category);

        return cb.run(
                webClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/places")
                                .queryParam("categories", category)
                                .queryParam("filter", getCityFilter(city))
                                .queryParam("limit", 10)
                                .queryParam("apiKey", apiKey)
                                .build())
                        .header("Authorization", "Bearer " + apiKey)
                        .retrieve()
                        .bodyToMono(GeoapifyResponse.class)
                        .doOnError(error -> log.error("Geoapify call failed: {}", error.getMessage()))
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                        .map(response -> response.features().stream()
                                .map(feature -> {
                                    List<String> cats = feature.properties().categories();
                                    String cat = (cats == null || cats.isEmpty()) ? "unknown" : cats.get(0);
                                    return new ActivityDto(
                                            feature.properties().name(),
                                            feature.properties().address_line1(),
                                            cat);
                                }).toList()),
                throwable -> {
                    log.error("Circuit breaker fallback triggered for activityApi: {}", throwable.getMessage());
                    return Mono.just(List.of());
                });
    }

    private String getCityFilter(String city) {
        return CITY_FILTERS.getOrDefault(city.toLowerCase(), "circle:18.0686,59.3293,5000");
    }

    // Ska mappa väder till Geoapify's kategorier!
    private String mapWeatherToCategory(String condition) {
        return switch (condition.toLowerCase()) {
            case "rain", "drizzle", "thunderstorm" -> "entertainment.museum";
            case "snow" -> "catering.cafe";
            case "sunny", "clear" -> "leisure.park";
            default -> "entertainment";
        };
    }

    // Private records för att tolka Geoapifys JSON svar!
    private record GeoapifyResponse(List<Feature> features) {
    }

    private record Feature(Properties properties) {
    }

    private record Properties(String name, String address_line1, List<String> categories) {
    }
}
