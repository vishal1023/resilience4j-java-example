package com.example.resilience4jexample.annotation.controller.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ClientController {

    private final RestTemplate restTemplate;

    public ClientController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "service")
    @Retry(name = "service1", fallbackMethod = "retryFallback")
    @GetMapping("/hello")
    public String getGreeting() {
        System.out.println("Getting Greetings from the client");
        ResponseEntity<String> response = this.restTemplate.getForEntity("http://localhost:8080/greet", String.class);
//        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
//        }
//        return "no greeting today";
    }

    public String retryFallback(Throwable e) {
        System.out.println("Retrying ... ");
        return "Retry default greetings";
    }

    public String handleException(Throwable e) {
        System.out.println("Circuit Breaker... ");
        return "exception case greetings";
    }
}

//In case of retry it will try number of times retry configured and if it's not successful it will return the fallback method.
