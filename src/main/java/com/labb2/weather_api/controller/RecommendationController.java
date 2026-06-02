package com.labb2.weather_api.controller;

import com.labb2.weather_api.dto.RecommendationResponse;
import com.labb2.weather_api.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Den här klassen är en REST-controller som tar emot GET-anrop på /api/recommendations
 * och skickar vidare till RecommendationService som sköter logiken
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor //Tack vare Lombok, undviker jag boilerplate!
public class RecommendationController {
    private final RecommendationService recommendationService;


    @GetMapping
    public Mono<RecommendationResponse> getRecommendations(@RequestParam String location) {
        return recommendationService.getRecommendations(location);
    }
}
