package com.clearn.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.clearn")
public class ClearnWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClearnWorkerApplication.class, args);
    }
}
