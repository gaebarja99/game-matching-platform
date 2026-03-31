package com.gamematcher.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final OAuth2RedirectOriginFilter oauth2RedirectOriginFilter;
    private final OAuth2KakaoNotConfiguredFilter oauth2KakaoNotConfiguredFilter;
    private final OAuth2NaverNotConfiguredFilter oauth2NaverNotConfiguredFilter;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
                          OAuth2RedirectOriginFilter oauth2RedirectOriginFilter,
                          OAuth2KakaoNotConfiguredFilter oauth2KakaoNotConfiguredFilter,
                          OAuth2NaverNotConfiguredFilter oauth2NaverNotConfiguredFilter) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        this.oauth2RedirectOriginFilter = oauth2RedirectOriginFilter;
        this.oauth2KakaoNotConfiguredFilter = oauth2KakaoNotConfiguredFilter;
        this.oauth2NaverNotConfiguredFilter = oauth2NaverNotConfiguredFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(s -> s.sessionFixation().changeSessionId())
            .addFilterBefore(oauth2RedirectOriginFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(oauth2KakaoNotConfiguredFilter, OAuth2AuthorizationRequestRedirectFilter.class)
            .addFilterBefore(oauth2NaverNotConfiguredFilter, OAuth2AuthorizationRequestRedirectFilter.class);
        if (clientRegistrationRepository != null) {
            // 기본 loginPage(/login) 이면 GET /login 이 Spring 기본 OAuth 선택 화면만 나와 React 라우트가 깨짐(새로고침 시).
            // 실제 로그인 UI는 SPA 의 /login — 미인증 시 리다이렉트용으로만 쓰이는 경로를 분리.
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2-login-page")
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(oAuth2LoginFailureHandler)
            );
        }
        return http.build();
    }
}
