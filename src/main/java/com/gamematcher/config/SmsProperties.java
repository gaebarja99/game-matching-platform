package com.gamematcher.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {
    private boolean enabled = false;
    private String provider = "naver-sens";
    private String baseUrl = "https://sens.apigw.ntruss.com";
    private String solapiBaseUrl = "https://api.solapi.com";
    private String serviceId = "";
    private String accessKey = "";
    private String secretKey = "";
    private String fromNumber = "";
    private String countryCode = "82";
}
