package com.labb2.weather_api.service;

import com.labb2.weather_api.client.ActivityClient;
import com.labb2.weather_api.client.WeatherClient;
import com.labb2.weather_api.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Den här klassen representerar en service-klass som kopplar ihop väder och aktiviteter
 * Den tar emot en stad, hämtar vädret och använder det för att hitta passande aktiviteter
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final WeatherClient weatherClient;
    private final ActivityClient activityClient;

    public Mono<RecommendationResponse> getRecommendations(String location) {
        return weatherClient.getWeather(location)  // Steg 1: Hämtar väderdata för staden
                .flatMap(weather -> activityClient.getActivities( // Steg 2: När vädret är hämtat, matcha aktiviteter
                        location,
                        weather.condition() // Skickar vidare väderförhållandet
                )
                        .map(activities ->  // Steg 3: bygg ihop svaret
                                new RecommendationResponse(
                                        weather.city(),
                                        weather.condition(),
                                        activities
                                )
                        )
                );
    }
}
