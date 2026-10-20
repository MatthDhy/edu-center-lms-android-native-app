package com.celehub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Storage storage, Bootstrap bootstrap) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Storage(String type, String localDir, String publicBaseUrl) {}
    public record Bootstrap(String adminEmail, String adminPassword) {}
}
