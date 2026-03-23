package com.gamematcher.config.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiotApiProperties {

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.regional-base-url}")
    private String regionalBaseUrl;

    @Value("${riot.api.platform-base-url}")
    private String platformBaseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getRegionalBaseUrl() {
        return regionalBaseUrl;
    }

    public String getPlatformBaseUrl() {
        return platformBaseUrl;
    }
}
