package com.celehub.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Storage storage, Bootstrap bootstrap, Mail mail, Supabase supabase) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Storage(String type, String localDir, String publicBaseUrl) {}
    public record Bootstrap(String adminEmail, String adminPassword) {}
    public record Mail(boolean enabled, String from) {}
    public record Supabase(String url, String serviceKey, String bucket) {}
}
