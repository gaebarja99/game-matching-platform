package com.gamematcher.config.auth;

import org.springframework.context.annotation.Configuration;

/**
 * 인증 연동 준비용 설정
 *
 * JWT 인증 도입 시 참고 사항:
 * 1. pom.xml에 spring-boot-starter-security, jjwt-api 의존성 추가
 * 2. JwtAuthenticationFilter 생성: Authorization 헤더에서 토큰 추출 후 SecurityContext 설정
 * 3. SecurityFilterChain에서 /api/** 는 인증 필요, /api/auth/login 등은 permitAll
 * 4. ReportBlockController의 @PathVariable userId → @AuthenticationPrincipal User 로 교체
 * 5. User 엔티티가 UserDetails 구현 또는 Principal에서 userId 추출
 */
@Configuration
public class AuthReadyConfig {
    // 현재 빈 설정 - 위 주석 참고하여 JWT 연동 시 구현
}
