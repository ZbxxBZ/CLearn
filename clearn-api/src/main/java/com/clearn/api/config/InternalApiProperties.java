package com.clearn.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clearn.security")
public record InternalApiProperties(String internalToken) {
}
