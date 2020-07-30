package com.example.resilience4jexample.api.controller.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.control.Try;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@RestController
public class ClientController {

    private final RestTemplate restTemplate;

    public ClientController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/hello")
    public String getGreeting() {
        System.out.println("Getting Greetings from the client");
//        ResponseEntity<String> response = this.restTemplate.getForEntity("http://localhost:8080/greet", String.class);
        return circuitBreaker();
    }

    public String circuitBreaker() {
        // Create a custom configuration for a CircuitBreaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .permittedNumberOfCallsInHalfOpenState(2)
                .slidingWindowSize(2)
                .recordExceptions(IOException.class, TimeoutException.class)
                .build();

        // Create a CircuitBreakerRegistry with a custom global configuration
        CircuitBreakerRegistry circuitBreakerRegistry =
                CircuitBreakerRegistry.of(circuitBreakerConfig);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry
                .circuitBreaker("name");

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> e instanceof Exception)
                .retryExceptions(IOException.class, TimeoutException.class, HttpStatusCodeException.class, ResourceAccessException.class)
                .build();

// Create a RetryRegistry with a custom global configuration
        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("name2", config);

        final Supplier<String> supplier = () ->
                this.restTemplate.getForObject("http://localhost:8090/greet", String.class);

        Supplier<String> decoratedSupplier = Decorators.ofSupplier(supplier)
//                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .decorate();

        String result = Try.ofSupplier(decoratedSupplier)
                .recover(throwable -> "Hello from Recovery").get();

        return result;
    }

    public String retryFallback(Throwable e) {
        System.out.println("Retrying ... ");
        return "Retry default greetings";
    }

    public String handleException(Throwable e) {
        System.out.println("Circuit Breaker... ");
        return "exception case greetings";
    }

    public CircuitBreaker config() {
        // Create a custom configuration for a CircuitBreaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(5)
//                .recordException(e -> INTERNAL_SERVER_ERROR
//                        .equals(getResponse().getStatus()))
                .recordExceptions(IOException.class, TimeoutException.class)
//                .ignoreExceptions(BusinessException.class, OtherBusinessException.class)
                .build();

// Create a CircuitBreakerRegistry with a custom global configuration
        CircuitBreakerRegistry circuitBreakerRegistry =
                CircuitBreakerRegistry.of(circuitBreakerConfig);

// Get or create a CircuitBreaker from the CircuitBreakerRegistry
// with the global default configuration
        return circuitBreakerRegistry.circuitBreaker("service");
    }
}

//In case of retry it will try number of times retry configured and if it's not successful it will return the fallback method.
