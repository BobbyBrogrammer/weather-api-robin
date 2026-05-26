package com.labb2.weather_api.dto;

/*
Den här recorden representerar väderdata för en stad
 */
public record WeatherData(String city, String condition, double temperatureCelsius) {}
