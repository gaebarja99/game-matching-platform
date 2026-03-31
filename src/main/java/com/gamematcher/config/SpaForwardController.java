package com.gamematcher.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * React Router(BrowserRouter) 경로로 직접 접근하거나 새로고침할 때 서버가 index.html을 내려주도록 함.
 * 그렇지 않으면 /login 등은 정적 리소스가 없어 404가 난다.
 * <p>Spring Boot 3 기본 PathPattern 은 이 Ant 스타일 다중 세그먼트 패턴을 지원하지 않으므로
 * application.properties 에 spring.mvc.pathmatch.matching-strategy=ant_path_matcher 가 있어야 한다.
 */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/{segment:[^\\.]+}",
            "/**/{segment:[^\\.]+}"
    })
    public String spaFallback(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (uri.startsWith("/api/")
                || uri.startsWith("/oauth2/")
                || uri.startsWith("/login/oauth2")
                || uri.startsWith("/h2-console")
                || uri.equals("/ws") || uri.startsWith("/ws/")
                || uri.startsWith("/hls/")
                || uri.startsWith("/uploads/")
                || uri.startsWith("/assets/")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}
