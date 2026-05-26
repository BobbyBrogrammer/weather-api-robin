package com.labb2.weather_api.dto;
import java.util.List;

/*
Den här recorden representerar väderinformation och en lista med aktivitetsrekommendationer
 */
public record RecommendationResponse(String city, String weather, List<ActivityDto> activities) {}
