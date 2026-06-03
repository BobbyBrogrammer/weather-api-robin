package com.labb2.weather_api.client;

import com.labb2.weather_api.dto.ActivityDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ActivityClientTest {

    static MockWebServer mockWebServer;

    @Autowired
    ActivityClient activityClient;

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
        registry.add("activity.api.url", () -> mockWebServer.url("/").toString());
        registry.add("activity.api.key", () -> "test-key");
        registry.add("resilience4j.timelimiter.instances.activityApi.timeout-duration", () -> "1s");
        registry.add("resilience4j.circuitbreaker.instances.activityApi.sliding-window-size", () -> "5");
        registry.add("resilience4j.circuitbreaker.instances.activityApi.failure-rate-threshold", () -> "60");
        registry.add("resilience4j.circuitbreaker.instances.activityApi.minimum-number-of-calls", () -> "5");
    }

    @Test
    @Order(1)
    void whenActivityApiSucceeds_thenActivitiesAreReturned() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                            "features": [{
                                "properties": {
                                    "name": "Test Museum",
                                    "address_line1": "Test Street 1",
                                    "categories": ["entertainment.museum"]
                                }
                            }]
                        }
                        """));

        List<ActivityDto> result = activityClient.getActivities("stockholm", "Rain").block();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Test Museum", result.get(0).name());
    }

    @Test
    @Order(2)
    void whenActivityApiTimesOut_thenStockholmFallbackIsReturned() {
        mockWebServer.enqueue(new MockResponse()
                .setHeadersDelay(2000, TimeUnit.MILLISECONDS));

        List<ActivityDto> result = activityClient.getActivities("stockholm", "Cloudy").block();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Skansen", result.get(0).name()); // Stockholm-specifik fallback
    }

    @Test
    @Order(3)
    void whenCircuitBreakerOpens_thenFallbackIsReturnedWithoutHttpCall() {
        // Tvinga circuit breaker att öppnas med 5 misslyckade anrop
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setHeadersDelay(2000, TimeUnit.MILLISECONDS));
            activityClient.getActivities("stockholm", "Cloudy").block();
        }

        // Circuit breaker är nu OPEN — nästa anrop ska inte göra ett HTTP-anrop alls
        int requestsBefore = mockWebServer.getRequestCount();
        List<ActivityDto> result = activityClient.getActivities("stockholm", "Cloudy").block();
        int requestsAfter = mockWebServer.getRequestCount();

        assertNotNull(result);
        assertEquals("Skansen", result.get(0).name());
        assertEquals(requestsBefore, requestsAfter); // Bevis: ingen ny HTTP-request gjordes!
    }
}
