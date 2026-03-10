package com.gamematcher.config.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValorantApiProperties {

    @Value("${valorant.api.base-url:https://api.henrikdev.xyz}")
    private String baseUrl;

    @Value("${valorant.api.key:}")
    private String apiKey;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
