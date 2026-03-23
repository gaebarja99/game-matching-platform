package com.gamematcher.config;

import jakarta.servlet.http.HttpSessionListener;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionListenerConfig {

    @Bean
    public ServletListenerRegistrationBean<HttpSessionListener> sessionCountListenerRegistration() {
        return new ServletListenerRegistrationBean<>(new SessionCountListener());
    }
}
