package com.gamematcher.service.ai;

import com.gamematcher.dto.chat.ChatDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final RestTemplate restTemplate;
    private final String ollamaBaseUrl;
    private final String model;

    public ChatbotServiceImpl(
            RestTemplate restTemplate,
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.model:llama3.1:8b}") String model
    ) {
        this.restTemplate = restTemplate;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.model = model;
    }

    @Override
    public ChatDto.ChatResponse chat(String userMessage) {
        String quickReply = quickReply(userMessage);
        if (quickReply != null) {
            return new ChatDto.ChatResponse(quickReply, true);
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", buildSystemPrompt()),
                        Map.of("role", "system", "content", buildFeatureGuide()),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    ollamaBaseUrl + "/api/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<String, Object> response = responseEntity.getBody();
            if (response == null) {
                return new ChatDto.ChatResponse("답변을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.", false);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) response.get("message");
            String reply = message != null ? String.valueOf(message.getOrDefault("content", "")).trim() : "";
            if (reply.isBlank()) {
                return new ChatDto.ChatResponse("질문을 조금 더 구체적으로 보내 주시면 안내를 도와드릴게요.", false);
            }

            return new ChatDto.ChatResponse(reply, true);
        } catch (Exception e) {
            return new ChatDto.ChatResponse(
                    "Ollama 연결에 실패했습니다. 로컬 Ollama가 실행 중인지와 모델 다운로드 상태를 확인해 주세요.",
                    false
            );
        }
    }

    private String quickReply(String userMessage) {
        String text = normalize(userMessage);

        if (containsAny(text, "전적", "기록", "레코드", "발로란트", "롤", "tft", "pubg", "오버워치", "apex", "cs2")) {
            return """
                    전적 검색은 메인 화면의 전적/기록 관련 메뉴에서 진행하면 됩니다.

                    1. 원하는 게임을 먼저 선택합니다.
                    2. 닉네임, 태그, 플랫폼 같은 필수 정보를 입력합니다.
                    3. 게임별로 최근 경기 수나 포지션 같은 옵션을 함께 고르면 더 정확하게 볼 수 있습니다.

                    예시:
                    - LoL / TFT: 닉네임 + 태그
                    - Valorant: 닉네임 + 태그 + 지역
                    - PUBG / Apex: 닉네임 또는 플랫폼 정보
                    - CS2: Steam 식별자

                    원하시면 어떤 게임 전적인지 말해 주시면 입력값 형식까지 바로 정리해 드릴게요.
                    """;
        }

        if (containsAny(text, "계정연동", "계정 연동", "연동", "discord", "steam", "blizzard", "riot")) {
            return """
                    계정 연동은 로그인 후 내 정보 영역에서 진행하는 흐름입니다.

                    연결 가능한 대표 계정:
                    - Discord
                    - Steam
                    - Blizzard
                    - Riot

                    보통은 연동 페이지에서 버튼을 눌러 OAuth 또는 수동 입력 방식으로 연결합니다.
                    연동이 안 되면 팝업 차단 여부와 로그인 상태를 먼저 확인해 주세요.
                    """;
        }

        if (containsAny(text, "커뮤니티", "게시글", "글쓰기", "질문", "공지")) {
            return """
                    커뮤니티에서는 글 조회, 글 작성, 정렬, 검색 같은 기능을 사용할 수 있습니다.

                    1. 원하는 게시판으로 이동합니다.
                    2. 검색 또는 정렬로 글을 찾습니다.
                    3. 로그인한 상태라면 글쓰기나 댓글 작성이 가능합니다.

                    필요하시면 어떤 종류의 글을 쓰고 싶은지에 맞춰 제목과 본문 예시도 도와드릴게요.
                    """;
        }

        if (containsAny(text, "내정보", "내 정보", "프로필", "마이페이지", "mypage")) {
            return """
                    내 정보 화면에서는 현재 계정 상태와 프로필 정보를 확인할 수 있습니다.

                    여기에서 보통 확인하는 항목:
                    - 닉네임
                    - 이메일
                    - 계정 상태
                    - 가입일
                    - 연동된 외부 계정
                    """;
        }

        if (containsAny(text, "신고", "차단", "block")) {
            return """
                    신고와 차단 기능은 안전 관련 메뉴 또는 해당 사용자와의 상호작용 화면에서 사용할 수 있습니다.

                    일반적으로는:
                    - 사용자를 신고하기
                    - 차단 목록 확인하기
                    - 필요 시 차단 해제하기

                    문제가 생긴 상황을 알려 주시면 어느 메뉴에서 처리하면 되는지 더 정확히 안내해 드릴게요.
                    """;
        }

        if (containsAny(text, "로그인", "회원가입", "가입")) {
            return """
                    로그인과 회원가입은 인증 화면에서 진행합니다.

                    - 기존 계정이 있으면 로그인
                    - 처음이면 회원가입 후 이용

                    로그인 후에는 프로필, 계정 연동, 채팅, 커뮤니티 같은 기능을 더 편하게 사용할 수 있습니다.
                    """;
        }

        return null;
    }

    private String buildSystemPrompt() {
        return """
                You are the in-app GameMatcher assistant.
                Reply in Korean unless the user explicitly asks for another language.
                Be concise, practical, and friendly.
                Focus on helping the user use the GameMatcher service.
                Do not invent features that are not described in the guidance.
                If you are unsure, tell the user which page to check instead.
                """;
    }

    private String buildFeatureGuide() {
        return """
                GameMatcher feature guide:
                - Main areas include home, streams, matching, esports, game rooms, group chat, profile, and studio.
                - Users can search player records and manage profile-related features.
                - There are community-style interactions such as chat, friends, and notifications.
                - Profile and account features may require login.
                - Safety features include report and block flows.
                """;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
