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
                        Map.of("role", "system", "content", buildRoutingHints()),
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
                return new ChatDto.ChatResponse("현재 답변을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.", false);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) response.get("message");
            String reply = message != null ? String.valueOf(message.getOrDefault("content", "")).trim() : "";

            if (reply.isBlank()) {
                return new ChatDto.ChatResponse("모델이 답변을 생성하지 못했습니다. 질문을 조금 더 구체적으로 보내 주세요.", false);
            }

            return new ChatDto.ChatResponse(reply, true);
        } catch (Exception e) {
            return new ChatDto.ChatResponse(
                    "Ollama 연결에 실패했습니다. Ollama 실행 여부와 모델 다운로드 상태를 확인해 주세요. " + e.getMessage(),
                    false
            );
        }
    }

    private String quickReply(String userMessage) {
        String text = normalize(userMessage);

        if (containsAny(text, "전적", "검색", "티어", "랭크", "발로란트", "롤", "tft", "pubg", "오버워치", "apex", "cs2")) {
            return """
                    전적 검색은 상단 메뉴의 `전적 검색` 페이지에서 할 수 있어요.

                    1. 원하는 게임을 먼저 선택하세요.
                    2. 닉네임을 입력하세요.
                    3. 게임에 따라 태그, 지역, 플랫폼, 최근 경기 수를 함께 넣으세요.
                    4. 검색을 누르면 플레이어 정보와 최근 경기 기록이 아래에 표시됩니다.

                    예를 들어:
                    - LoL / TFT: 닉네임 + 태그
                    - Valorant: 닉네임 + 태그 + 지역
                    - PUBG / Apex: 닉네임 + 플랫폼
                    - CS2: Steam64 ID 또는 Steam Vanity URL

                    원하면 어떤 게임 전적인지 말해주시면 입력값도 바로 알려드릴게요.
                    """;
        }

        if (containsAny(text, "계정연동", "계정 연동", "연동", "디스코드", "스팀", "블리자드", "라이엇")) {
            return """
                    계정 연동은 로그인 후 상단 메뉴의 `계정 연동` 페이지에서 진행할 수 있어요.

                    현재 연동 가능한 대상:
                    - Discord
                    - Steam
                    - Blizzard
                    - Riot

                    사용 방법:
                    1. 먼저 로그인합니다.
                    2. `계정 연동` 페이지로 이동합니다.
                    3. 원하는 플랫폼의 연동 버튼을 누릅니다.
                    4. Riot은 수동 입력 방식, 나머지는 OAuth 팝업 방식으로 연결됩니다.

                    연동이 안 되면 팝업 차단 여부도 같이 확인해 주세요.
                    """;
        }

        if (containsAny(text, "커뮤니티", "글쓰기", "게시글", "게시판", "공지", "질문")) {
            return """
                    커뮤니티는 상단 메뉴의 `커뮤니티` 페이지에서 이용할 수 있어요.

                    가능한 기능:
                    - 카테고리별 게시글 보기
                    - 검색어로 글 찾기
                    - 최신순 / 인기순 / 조회순 정렬
                    - 게시글 상세 보기
                    - 로그인 후 글쓰기

                    글을 쓰려면:
                    1. 로그인합니다.
                    2. 커뮤니티 페이지로 이동합니다.
                    3. `글쓰기` 버튼을 눌러 작성 페이지로 들어갑니다.

                    원하면 어떤 종류의 글을 쓰고 싶은지도 같이 정리해드릴게요.
                    """;
        }

        if (containsAny(text, "내 정보", "프로필", "마이페이지", "my page", "me")) {
            return """
                    `내 정보` 페이지에서는 현재 계정 상태를 확인할 수 있어요.

                    여기서 볼 수 있는 정보:
                    - 닉네임
                    - 이메일
                    - 상태
                    - 가입 시각
                    - 연결된 외부 계정 목록

                    외부 계정 연결이나 해제는 `계정 연동` 페이지에서 이어서 진행하면 됩니다.
                    """;
        }

        if (containsAny(text, "신고", "차단", "블락", "block")) {
            return """
                    신고와 차단 기능은 `신고·차단` 메뉴에서 확인할 수 있어요.

                    일반적으로는:
                    - 사용자 신고 등록
                    - 내가 한 신고 조회
                    - 차단 사용자 관리
                    같은 흐름으로 사용합니다.

                    관리자 계정이라면 별도로 `신고 관리` 페이지에서 처리 상태를 확인할 수 있어요.
                    """;
        }

        if (containsAny(text, "로그인", "회원가입", "가입")) {
            return """
                    로그인과 회원가입은 `로그인` 페이지에서 할 수 있어요.

                    - 기존 계정이 있으면 로그인 탭 사용
                    - 처음이면 회원가입 탭에서 아이디, 비밀번호, 닉네임, 이메일을 입력

                    로그인 후에는 내 정보, 계정 연동, 커뮤니티 글쓰기 같은 기능을 사용할 수 있습니다.
                    """;
        }

        return null;
    }

    private String buildSystemPrompt() {
        return """
                You are the in-app GameMatcher assistant.
                Reply in Korean unless the user explicitly asks for another language.
                Be concise, practical, and friendly.
                Focus on helping the user use the GameMatcher website.
                Do not invent features that were not described in the provided guidance.
                If you are unsure, say what page the user should check instead of hallucinating.
                """;
    }

    private String buildFeatureGuide() {
        return """
                GameMatcher feature guide:
                - Main navigation includes 홈, 전적 검색, 커뮤니티, 챗봇, 내 정보, 계정 연동, 신고·차단.
                - Search page supports LoL, TFT, Valorant, PUBG, Overwatch 2, Apex Legends, and CS2.
                - Search inputs vary by game: nickname, tag, region, platform, and recent match count.
                - Community page supports category filters, keyword search, sorting, post detail, and authenticated post writing.
                - My profile page shows nickname, email, status, created time, and linked external accounts.
                - Account connections page supports Discord, Steam, Blizzard, and Riot.
                - Riot linking is manual, other providers use OAuth popup flow.
                - Safety pages are used for reports, blocks, and admin report handling.
                """;
    }

    private String buildRoutingHints() {
        return """
                Routing hints for the assistant:
                - For search questions, direct users to the 전적 검색 page.
                - For posting and browsing discussion, direct users to 커뮤니티.
                - For linked account help, direct users to 계정 연동.
                - For profile checks, direct users to 내 정보.
                - For report and block questions, direct users to 신고·차단.
                - Mention login when the feature requires authentication.
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
