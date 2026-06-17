package com.clearn.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.clearn")
public class ClearnApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClearnApiApplication.class, args);
    }
}
