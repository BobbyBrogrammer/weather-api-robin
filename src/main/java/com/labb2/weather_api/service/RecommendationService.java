package com.labb2.weather_api.service;

import com.labb2.weather_api.dto.RecommendationResponse;
import reactor.core.publisher.Mono;

public class RecommendationService {

    public Mono<RecommendationResponse> getRecommendations(String location) {
        return Mono.empty(); //Platshållare tills jag adderar riktig logik, vilket jag gör i steg 2&3.
        //Jag gör atomära commits för att få en bättre dokumentation.
    }
}
