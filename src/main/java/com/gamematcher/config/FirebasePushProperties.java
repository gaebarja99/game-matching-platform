package com.gamematcher.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.firebase.push")
public class FirebasePushProperties {
    private boolean enabled = false;
    private String serviceAccountPath = "";
}

