package com.labb2.weather_api.client;

import com.labb2.weather_api.dto.WeatherData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class WeatherClientTest {

    static MockWebServer mockWebServer;

    @Autowired
    WeatherClient weatherClient;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("weather.api.url", () -> mockWebServer.url("/").toString());
        registry.add("weather.api.key", () -> "test-key");
        registry.add("resilience4j.timelimiter.instances.weatherApi.timeout-duration", () -> "200ms");
    }

    @Test
    void whenWeatherApiSucceeds_thenCorrectDataIsReturned() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                            "location": {"name": "Stockholm"},
                            "current": {
                                "temp_c": 20.0,
                                "condition": {"text": "Sunny"}
                            }
                        }
                        """));

        WeatherData result = weatherClient.getWeather("Stockholm").block();

        assertNotNull(result);
        assertEquals("Stockholm", result.city());
        assertEquals("Sunny", result.condition());
        assertEquals(20.0, result.temperatureCelsius());
    }

    @Test
    void whenWeatherApiTimesOut_thenSunnyFallbackIsReturned() {
        mockWebServer.enqueue(new MockResponse()
                .setHeadersDelay(500, TimeUnit.MILLISECONDS));

        WeatherData result = weatherClient.getWeather("Stockholm").block();

        assertNotNull(result);
        assertEquals("Unknown", result.city());
        assertEquals("Sunny", result.condition()); // Vår definierade fallback
    }
}
