package com.example.resilience4jexample.annotation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.*;

@RestController
public class HelloWorldController {

    @GetMapping("/greet")
    public ResponseEntity<String> greet() {
        return new ResponseEntity<>("Hello", OK);
    }
}
