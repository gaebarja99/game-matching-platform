package com.gamematcher.config.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiotApiProperties {

    /** 비어 있으면 앱은 기동되나 Riot API 호출은 실패할 수 있음. 실제 키는 환경변수/로컬 properties에 설정. */
    @Value("${riot.api.key:}")
    private String apiKey;

    @Value("${riot.api.regional-base-url:https://asia.api.riotgames.com}")
    private String regionalBaseUrl;

    @Value("${riot.api.platform-base-url:https://kr.api.riotgames.com}")
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

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
