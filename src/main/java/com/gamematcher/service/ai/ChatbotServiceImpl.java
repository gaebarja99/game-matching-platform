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

    private static final String ASSISTANT_NAME = "GM Mate";
    private static final String OUT_OF_SCOPE_REPLY = "도와드릴 내용이 있으면 말씀해 주세요.";

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
                return new ChatDto.ChatResponse(defaultFallback(userMessage), false);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) response.get("message");
            String reply = message != null ? String.valueOf(message.getOrDefault("content", "")).trim() : "";
            if (reply.isBlank()) {
                return new ChatDto.ChatResponse(defaultFallback(userMessage), false);
            }

            return new ChatDto.ChatResponse(sanitizeReply(reply), true);
        } catch (Exception e) {
            return new ChatDto.ChatResponse(defaultFallback(userMessage), false);
        }
    }

    private String quickReply(String userMessage) {
        String text = normalize(userMessage);

        if (text.isBlank()) {
            return formatGuide(
                    "필요한 기능만 짧게 보내 주세요.",
                    List.of(
                            "전적 검색 방법",
                            "계정 연동",
                            "채팅, 후원, 스튜디오"
                    ),
                    List.of(
                            "하고 싶은 작업을 한 줄로 보내 주세요.",
                            "예: 발로란트 태그 입력, 계정 연동 위치, 후원 방법"
                    )
            );
        }

        String reply = recordSpecificReply(text);
        if (reply != null) return reply;

        reply = accountLinkReply(text);
        if (reply != null) return reply;

        reply = profileReply(text);
        if (reply != null) return reply;

        reply = communityReply(text);
        if (reply != null) return reply;

        reply = chatReply(text);
        if (reply != null) return reply;

        reply = safetyReply(text);
        if (reply != null) return reply;

        reply = paymentReply(text);
        if (reply != null) return reply;

        reply = studioReply(text);
        if (reply != null) return reply;

        if (isIdentityQuestion(text)) {
            return """
                    저는 `%s`예요. GameMatcher 사용법만 빠르게 안내해 드려요.

                    📌 바로 도와드릴 수 있는 내용
                    - 전적 검색 입력 형식
                    - 계정 연동, 채팅, 후원, 스튜디오
                    - 오류 확인 포인트

                    👉 예시
                    - "발로란트 태그는 어떻게 입력해?"
                    - "계정 연동이 안 되면 뭐부터 봐야 해?"
                    """.formatted(ASSISTANT_NAME);
        }

        if (isServiceQuestion(text)) {
            return """
                    💡 이 채팅은 GameMatcher 안내용 챗봇이에요.

                    📌 도와드릴 수 있는 범위
                    - 전적 검색
                    - 계정 연동, 채팅, 후원, 스튜디오
                    - 오류 확인 포인트

                    👉 하려는 작업만 보내 주시면 바로 안내해 드릴게요.
                    예: "PUBG 검색하려고 하는데 뭐 입력해?"
                    """;
        }

        if (containsAny(text, "기능", "뭐할수있어", "뭐할수있냐", "무슨기능", "서비스")) {
            return """
                    GameMatcher에서는 이런 걸 할 수 있어요.

                    - LoL, TFT, Valorant, PUBG, Overwatch 2, CS2 전적 검색
                    - Discord, Steam, Blizzard, Riot 외부 계정 연동
                    - 프로필 관리와 내 정보 변경
                    - 커뮤니티 글 목록, 글 작성, 글 조회
                    - 1:1 채팅, 단체 채팅, 랜덤 매칭 채팅
                    - 친구 기반 DM 흐름과 알림 확인
                    - 신고, 차단 같은 안전 기능
                    - 팡, 구독, 광고 제거, 마일리지 같은 결제 영역
                    - 방송/스튜디오 관리 화면

                    다음 행동:
                    - 전적을 보려면 전적검색 페이지에서 게임과 닉네임부터 넣어 보세요.
                    - 계정 연동이 목적이면 프로필 > 외부 계정 연동으로 가면 됩니다.
                    """;
        }

        if (containsAny(text, "안됨", "안돼", "실패", "오류", "문제", "먹통")) {
            return formatGuide(
                    "오류가 있을 때는 원인부터 좁히는 게 가장 빨라요.",
                    List.of(
                            "입력값 형식이 맞는지 확인",
                            "플랫폼, 태그, 지역 같은 선택값 확인",
                            "로그인이나 계정 연동이 필요한 기능인지 확인",
                            "현재 메뉴가 맞는지 확인",
                            "화면에 뜬 오류 문구 확인"
                    ),
                    List.of(
                            "어떤 기능에서 막혔는지와 오류 문구를 같이 보내 주세요.",
                            "가능하면 게임명, 입력값, 선택한 플랫폼도 함께 알려 주세요."
                    )
            );
        }

        if (containsAny(text, "로그인", "회원가입", "가입", "로그아웃")) {
            return formatGuide(
                    "로그인은 주요 기능을 쓰기 전에 먼저 필요한 경우가 많아요.",
                    List.of(
                            "프로필",
                            "외부 계정 연동",
                            "커뮤니티 글쓰기",
                            "DM과 결제, 스튜디오"
                    ),
                    List.of(
                            "로그인 화면에서 먼저 인증을 완료해 주세요.",
                            "로그인한 상태인데도 막히면 현재 화면과 오류 문구를 보내 주세요."
                    )
            );
        }

        if (containsAny(text, "고마워", "감사", "thanks")) {
            return """
                    계속 이어서 도와드릴 수 있어요.

                    다음 행동:
                    - 지금 막힌 기능 하나만 말해 주시면 바로 다음 단계부터 같이 볼게요.
                    """;
        }

        if (containsAny(text, "안녕", "하이", "반가", "hello")) {
            return """
                    👋 안녕하세요. `%s`예요.

                    전적 검색, 계정 연동, 채팅, 후원, 스튜디오 안내를 도와드려요.
                    👉 필요한 기능만 한 줄로 보내 주세요.
                    """.formatted(ASSISTANT_NAME);
        }

        if (isOutOfScopeConversation(text)) {
            return OUT_OF_SCOPE_REPLY;
        }

        return null;
    }

    private String recordSpecificReply(String text) {
        if (containsAny(text, "롤", "리그오브레전드", "lol")) {
            return formatGuide(
                    "LoL 전적 검색은 전적검색 페이지에서 LoL을 고른 뒤 게임 이름과 태그를 입력하면 돼요.",
                    List.of(
                            "입력 예시: 게임 이름 `Hide on bush`, 태그 `KR1`",
                            "태그까지 함께 넣어야 검색이 정확해요.",
                            "최근 20매치 기준으로 전적과 핵심 지표가 보여요."
                    ),
                    List.of(
                            "전적검색 페이지에서 LoL을 선택하고 이름과 태그를 입력해 보세요.",
                            "태그를 모르겠으면 라이엇 계정 표기를 그대로 확인해 주세요."
                    )
            );
        }

        if (containsAny(text, "tft", "전략적팀전투")) {
            return """
                    TFT 전적 검색도 LoL과 비슷하게 게임 이름과 태그를 같이 입력하면 됩니다.

                    입력 방식:
                    - 게임 이름
                    - 태그

                    참고:
                    - 최근 20매치와 순위 흐름 중심으로 보여요.
                    - 태그가 빠지면 원하는 계정을 찾지 못할 수 있어요.

                    다음 행동:
                    - 전적검색 페이지에서 TFT를 선택하고 이름과 태그를 입력해 보세요.
                    """;
        }

        if (containsAny(text, "발로란트", "valorant")) {
            return formatGuide(
                    "발로란트 전적 검색은 플레이어 이름과 태그를 같이 넣는 흐름이에요.",
                    List.of(
                            "입력 예시: 플레이어 이름 `TenZ`, 태그 `KR1`",
                            "이름과 태그를 함께 넣어야 해요.",
                            "지역 선택이 필요한 경우 Korea, AP, NA 같은 옵션을 확인해 주세요."
                    ),
                    List.of(
                            "태그가 헷갈리면 라이엇 계정 표기를 그대로 확인해 주세요.",
                            "검색 실패 시 이름, 태그, 지역을 함께 다시 점검해 보세요."
                    )
            );
        }

        if (containsAny(text, "배그", "pubg", "카카오", "steam", "스팀")) {
            return """
                    PUBG 전적 검색은 닉네임과 플랫폼 선택이 같이 맞아야 해요.

                    입력 방식:
                    - 닉네임 입력
                    - 플랫폼 선택: Steam 또는 Kakao

                    체크 포인트:
                    - Steam 계정이면 Steam, Kakao 계정이면 Kakao를 선택해야 해요.
                    - 닉네임 철자와 특수문자도 그대로 맞춰야 합니다.

                    다음 행동:
                    - 먼저 내 계정이 Steam인지 Kakao인지 확인해 주세요.
                    - 계속 안 되면 입력한 닉네임과 선택한 플랫폼을 같이 알려 주세요.
                    """;
        }

        if (containsAny(text, "오버워치", "오버워치2", "overwatch", "배틀태그", "battletag")) {
            return """
                    오버워치 2 전적 검색은 배틀태그 이름과 숫자 태그를 함께 넣는 방식이에요.

                    입력 예시:
                    - 이름: PlayerName
                    - 배틀태그: 1234

                    체크 포인트:
                    - 이름만 넣지 말고 숫자 태그까지 함께 확인해 주세요.
                    - Battle.net 계정 표기와 다르면 검색이 안 될 수 있어요.

                    다음 행동:
                    - 오버워치 2를 선택하고 이름과 숫자 태그를 분리해서 넣어 보세요.
                    """;
        }

        if (containsAny(text, "cs2", "카스", "카운터스트라이크", "steam64", "vanityurl")) {
            return """
                    CS2 전적 검색은 Steam64 ID 또는 Steam vanity URL 기준으로 진행돼요.

                    입력 방식:
                    - Steam64 ID
                    - 또는 Steam vanity URL 식별자

                    체크 포인트:
                    - 프로필 전체 주소보다 식별자만 넣는 쪽이 안전해요.
                    - 닉네임만으로는 찾기 어려울 수 있어요.

                    다음 행동:
                    - 스팀 프로필 식별자를 확인해서 전적검색 페이지에 입력해 보세요.
                    - 무엇을 넣어야 할지 헷갈리면 현재 가진 스팀 정보 형태를 보내 주세요.
                    """;
        }

        if (containsAny(text, "전적", "기록", "전적검색", "검색실패", "플레이어를찾을수없", "찾을수없")) {
            return formatGuide(
                    "전적 검색은 게임마다 입력 형식이 조금 달라요.",
                    List.of(
                            "LoL / TFT: 게임 이름 + 태그",
                            "Valorant: 플레이어 이름 + 태그",
                            "PUBG: 닉네임 + 플랫폼 선택",
                            "Overwatch 2: 배틀태그 이름 + 숫자 태그",
                            "CS2: Steam64 ID 또는 vanity URL"
                    ),
                    List.of(
                            "원하는 게임명을 말해 주시면 입력 예시를 바로 맞춰서 알려드릴게요.",
                            "검색 실패 중이면 오류 문구까지 같이 보내 주세요."
                    )
            );
        }

        return null;
    }

    private String accountLinkReply(String text) {
        if (!containsAny(text, "연동", "계정연동", "디스코드", "discord", "스팀", "steam", "블리자드", "blizzard", "라이엇", "riot", "연동상태", "미연동")) {
            return null;
        }

        if (containsAny(text, "디스코드", "discord")) {
            return """
                    Discord 계정 연동은 프로필 > 외부 계정 연동 화면에서 시작하면 돼요.

                    흐름:
                    - 로그인 상태 확인
                    - 외부 계정 연동 화면 이동
                    - Discord 연동 버튼 클릭
                    - 인증 창에서 연결 승인

                    다음 행동:
                    - 프로필 > 외부 계정 연동에서 Discord 연동을 시작해 보세요.
                    - 팝업이 막히면 브라우저 팝업 허용도 함께 확인해 주세요.
                    """;
        }

        if (containsAny(text, "스팀", "steam")) {
            return """
                    Steam 계정 연동도 프로필 > 외부 계정 연동에서 진행해요.

                    흐름:
                    - Steam 연동 버튼 클릭
                    - 스팀 인증 창에서 승인
                    - 연동 상태와 표시 이름 확인

                    다음 행동:
                    - 스팀 계정으로 로그인 가능한지 먼저 확인해 주세요.
                    - 연동 후에는 같은 화면에서 상태를 볼 수 있어요.
                    """;
        }

        if (containsAny(text, "블리자드", "blizzard", "배틀넷", "battlenet")) {
            return """
                    Blizzard 계정은 프로필 > 외부 계정 연동에서 Battle.net 인증 흐름으로 연결하면 됩니다.

                    체크 포인트:
                    - 로그인 상태 필요
                    - 인증 팝업 허용 필요

                    다음 행동:
                    - Blizzard 연동 버튼을 눌러 연결을 시작해 보세요.
                    """;
        }

        if (containsAny(text, "라이엇", "riot")) {
            return """
                    Riot 계정 연동은 팝업 인증이 아니라 게임명과 태그를 직접 입력하는 방식이에요.

                    입력 방식:
                    - 게임명
                    - 태그

                    다음 행동:
                    - 프로필 > 외부 계정 연동 화면에서 Riot 항목에 게임명과 태그를 넣어 보세요.
                    - 입력값은 라이엇 계정 표기와 정확히 맞춰 주세요.
                    """;
        }

        if (containsAny(text, "연동안됨", "연동실패", "연동상태", "미연동", "연동확인")) {
            return formatGuide(
                    "외부 계정 연동 상태는 프로필 > 외부 계정 연동 화면에서 바로 확인할 수 있어요.",
                    List.of(
                            "연동됨 / 미연동 상태 확인",
                            "표시 이름 확인",
                            "본인 확인 여부 확인",
                            "로그인 상태, 팝업 차단, Riot 태그 입력값 점검"
                    ),
                    List.of(
                            "프로필 > 외부 계정 연동에서 상태를 먼저 확인해 보세요.",
                            "문제된 제공자 이름과 오류 문구를 알려 주시면 더 구체적으로 봐드릴게요."
                    )
            );
        }

        return """
                외부 계정 연동은 프로필 > 외부 계정 연동 화면에서 관리해요.

                연결 가능한 계정:
                - Discord
                - Steam
                - Blizzard
                - Riot

                다음 행동:
                - Discord, Steam, Blizzard는 연동 버튼으로 인증
                - Riot은 게임명과 태그 직접 입력
                - 연동 상태는 같은 화면에서 바로 확인
                """;
    }

    private String profileReply(String text) {
        if (!containsAny(text, "프로필", "내정보", "마이페이지", "닉네임", "소개", "자기소개", "비밀번호", "전화번호", "이메일", "회원정보")) {
            return null;
        }

        if (containsAny(text, "닉네임", "소개", "자기소개", "프로필수정", "프로필편집")) {
            return """
                    프로필 홈에서는 닉네임, 자기소개, 프로필 사진 같은 기본 프로필을 수정할 수 있어요.

                    흐름:
                    - 프로필 홈으로 이동
                    - 프로필 편집 버튼 클릭
                    - 닉네임, 소개, 사진 변경

                    체크 포인트:
                    - 닉네임 변경 시 중복확인이 필요할 수 있어요.

                    다음 행동:
                    - 바꾸려는 항목이 닉네임인지 소개인지 알려 주시면 더 짧게 안내해 드릴게요.
                    """;
        }

        if (containsAny(text, "비밀번호", "전화번호", "이메일", "이름", "내정보")) {
            return """
                    비밀번호, 전화번호, 이름 같은 계정 정보는 프로필 > 내 정보 메뉴에서 관리하는 흐름이에요.

                    내 정보에서 할 수 있는 일:
                    - 비밀번호 변경
                    - 이름 변경
                    - 전화번호 변경과 인증
                    - 계정 탈퇴 관련 절차

                    다음 행동:
                    - 프로필 > 내 정보로 이동해 원하는 항목을 선택해 보세요.
                    """;
        }

        return """
                프로필 관련 작업은 보통 프로필 홈과 내 정보 메뉴에서 나뉘어요.

                정리하면:
                - 프로필 홈: 닉네임, 자기소개, 사진, 팡/마일리지 상태
                - 내 정보: 비밀번호, 이름, 전화번호 같은 계정 정보
                - 외부 계정 연동: Discord, Steam, Blizzard, Riot 상태 확인

                다음 행동:
                - 무엇을 바꾸고 싶은지 한 가지만 말해 주시면 정확한 메뉴로 바로 안내할게요.
                """;
    }

    private String communityReply(String text) {
        if (!containsAny(text, "커뮤니티", "게시글", "글쓰기", "글작성", "게시판", "공지", "댓글", "답글", "북마크", "좋아요")) {
            return null;
        }

        if (containsAny(text, "글쓰기", "글작성", "작성")) {
            return formatGuide(
                    "커뮤니티 글 작성은 커뮤니티 화면의 글쓰기 버튼으로 시작하면 됩니다.",
                    List.of(
                            "로그인 상태 확인",
                            "게시판 선택",
                            "제목과 내용 입력",
                            "이미지와 해시태그는 선택 추가"
                    ),
                    List.of(
                            "커뮤니티 > 글쓰기로 이동해서 작성해 보세요.",
                            "기존 글 수정이면 수정 권한이 있는 본인 글인지도 함께 확인해 주세요."
                    )
            );
        }

        if (containsAny(text, "목록", "게시판", "카테고리", "공지")) {
            return """
                    커뮤니티에서는 글 목록과 게시판 카테고리를 함께 볼 수 있어요.

                    가능한 흐름:
                    - 전체 / 공지 / 자유 / 질문
                    - 게임별 게시판
                    - 검색과 정렬

                    다음 행동:
                    - 커뮤니티 페이지에서 원하는 게시판 탭을 먼저 선택해 보세요.
                    """;
        }

        if (containsAny(text, "게시글", "상세", "조회", "댓글", "답글", "좋아요", "북마크")) {
            return """
                    게시글 상세 화면에서는 글 내용 확인과 상호작용을 같이 할 수 있어요.

                    가능한 기능:
                    - 게시글 상세 조회
                    - 댓글과 답글 작성
                    - 좋아요, 북마크
                    - 신고

                    다음 행동:
                    - 커뮤니티 목록에서 글을 눌러 상세 화면으로 들어가 보세요.
                    """;
        }

        return """
                커뮤니티에서는 글 목록 확인, 글 작성, 글 상세 조회까지 모두 할 수 있어요.

                다음 행동:
                - 글을 쓰려면 커뮤니티 > 글쓰기
                - 글을 보려면 커뮤니티 목록에서 게시글 선택
                - 댓글이나 신고는 게시글 상세 화면에서 진행
                """;
    }

    private String chatReply(String text) {
        if (!containsAny(text, "채팅", "dm", "친구", "알림", "1:1", "단체채팅", "랜덤채팅", "게임방", "매칭")) {
            return null;
        }

        if (containsAny(text, "1:1", "dm", "친구")) {
            return formatGuide(
                    "1:1 채팅은 친구 기반 DM 흐름으로 사용하는 구조예요.",
                    List.of(
                            "로그인",
                            "DM 또는 친구 목록 진입",
                            "친구 선택",
                            "대화방에서 메시지 전송",
                            "최근 대화, 읽지 않은 메시지, 친구 프로필 확인 가능"
                    ),
                    List.of(
                            "DM 화면이나 우측 하단 채팅 위젯에서 1:1 채팅을 열어 보세요.",
                            "안 보이면 친구 목록이나 로그인 상태부터 확인해 주세요."
                    )
            );
        }

        if (containsAny(text, "단체채팅", "그룹채팅", "groupchat")) {
            return """
                    단체 채팅은 참여 중인 방 목록에서 들어가는 흐름이에요.

                    흐름:
                    - 단체 채팅 메뉴 진입
                    - 참여 중인 방 확인
                    - 원하는 방 선택 후 입장

                    다음 행동:
                    - 단체 채팅 페이지나 플로팅 채팅 위젯에서 방 목록을 확인해 보세요.
                    """;
        }

        if (containsAny(text, "랜덤채팅", "랜덤매칭", "랜덤")) {
            return """
                    랜덤 매칭 채팅은 플로팅 채팅 위젯 안의 랜덤 채팅 흐름으로 접근하는 구조예요.

                    다음 행동:
                    - 우측 하단 채팅 위젯을 열고 랜덤 채팅 탭을 확인해 보세요.
                    - 게임방 매칭이 목적이면 게임방 기능과 같이 보는 것도 좋아요.
                    """;
        }

        if (containsAny(text, "게임방", "매칭")) {
            return """
                    매칭용 게임방은 게임방 화면에서 만들고 참여하는 흐름이에요.

                    가능한 작업:
                    - 게임별 방 목록 보기
                    - 방 만들기
                    - 참여 후 채팅방 연결

                    체크 포인트:
                    - 방 생성 시 제목과 삭제용 비밀번호가 필요할 수 있어요.

                    다음 행동:
                    - 게임방 페이지에서 원하는 게임 필터를 고르고 방을 만들거나 참여해 보세요.
                    """;
        }

        if (containsAny(text, "알림", "읽지않은", "unread", "메시지알림")) {
            return """
                    읽지 않은 메시지나 채팅 알림은 DM 흐름과 플로팅 위젯에서 확인하는 구조예요.

                    다음 행동:
                    - DM 화면에서 최근 대화와 unread 수를 확인해 보세요.
                    - 우측 하단 위젯에서도 읽지 않은 상태를 볼 수 있어요.
                    """;
        }

        return """
                채팅 관련 기능은 1:1 채팅, 단체 채팅, 랜덤 채팅, 게임방 매칭 흐름으로 나뉘어요.

                다음 행동:
                - 친구와 대화면 DM
                - 여러 명 대화면 단체 채팅
                - 매칭 성격이면 게임방이나 랜덤 채팅
                """;
    }

    private String safetyReply(String text) {
        if (!containsAny(text, "신고", "차단", "block", "report")) {
            return null;
        }

        if (containsAny(text, "신고")) {
            return """
                    신고는 커뮤니티 게시글이나 댓글 같은 상호작용 화면에서 접수하는 흐름이에요.

                    보통 가능한 곳:
                    - 게시글 상세
                    - 댓글/답글 영역

                    다음 행동:
                    - 문제가 된 대상이 게시글인지 댓글인지 알려 주시면 가장 가까운 위치로 안내할게요.
                    """;
        }

        return """
                차단은 친구 기반 1:1 채팅 흐름에서 바로 연결되는 기능이 있어요.

                보통 흐름:
                - 친구 프로필 열기
                - 차단 버튼 선택
                - 차단 시 친구 관계 해제 여부 확인

                참고:
                - 스튜디오에는 별도로 시청자 차단 목록 관리 화면도 있어요.

                다음 행동:
                - 친구를 차단하려는지, 방송 시청자를 차단하려는지 알려 주시면 메뉴를 정확히 짚어드릴게요.
                """;
    }

    private String paymentReply(String text) {
        if (!containsAny(text, "후원", "결제", "팡", "광고제거", "광고제거", "광고", "마일리지", "구독", "환불")) {
            return null;
        }

        if (containsAny(text, "팡", "충전", "환불")) {
            return formatGuide(
                    "팡 관련 기능은 프로필 > 팡 메뉴에서 확인하는 흐름이에요.",
                    List.of(
                            "팡 잔액 확인",
                            "팡 충전",
                            "충전/사용 내역 조회",
                            "환불 가능한 건 환불 요청"
                    ),
                    List.of(
                            "프로필 > 팡으로 이동해서 잔액과 내역을 먼저 확인해 보세요.",
                            "결제 오류가 있으면 주문 정보나 오류 문구도 같이 확인해 주세요."
                    )
            );
        }

        if (containsAny(text, "마일리지")) {
            return """
                    마일리지는 프로필 홈과 마일리지 상점 흐름에서 확인하는 구조예요.

                    참고:
                    - 결제 금액 일부가 적립되는 구조가 연결돼 있을 수 있어요.

                    다음 행동:
                    - 프로필에서 현재 마일리지를 확인하고, 사용이 목적이면 마일리지 상점도 같이 확인해 보세요.
                    """;
        }

        if (containsAny(text, "구독")) {
            return """
                    구독 관련 정보는 프로필 > 구독 메뉴에서 확인하는 흐름이에요.

                    가능한 기능:
                    - 내가 구독 중인 스트리머 목록 확인
                    - 채널로 이동

                    다음 행동:
                    - 프로필 > 구독 메뉴에서 구독 중인 목록을 확인해 보세요.
                    """;
        }

        if (containsAny(text, "광고제거", "광고 제거", "adfree")) {
            return """
                    광고 제거는 프로필 > 광고 제거 메뉴에서 구매와 적용 상태를 확인할 수 있어요.

                    가능한 기능:
                    - 광고 제거 적용 여부 확인
                    - 만료일 확인
                    - 30일권 구매

                    다음 행동:
                    - 프로필 > 광고 제거로 이동해서 현재 상태와 구매 버튼을 확인해 보세요.
                    """;
        }

        return """
                후원과 결제 관련 흐름은 프로필의 팡, 구독, 광고 제거 메뉴와 연결돼 있어요.

                정리하면:
                - 팡 관련 확인: 프로필 > 팡
                - 구독 확인: 프로필 > 구독
                - 광고 제거 구매: 프로필 > 광고 제거
                - 마일리지 관련 확인: 프로필 또는 마일리지 상점

                다음 행동:
                - 어떤 결제 흐름인지 한 가지만 알려 주시면 더 정확히 안내할게요.
                """;
    }

    private String studioReply(String text) {
        if (!containsAny(text, "방송", "스트림", "스트리밍", "스튜디오", "obs", "스트림키", "알림", "채팅설정", "금칙어", "클린봇", "수익", "시청자차단", "차단목록")) {
            return null;
        }

        if (containsAny(text, "방송", "스트림", "스트리밍", "obs", "스트림키")) {
            return formatGuide(
                    "방송 시작은 스튜디오의 방송 관리 흐름에서 진행해요.",
                    List.of(
                            "스튜디오 진입",
                            "방송하기 화면 이동",
                            "방송 등록 후 스트림 키 확인",
                            "OBS 같은 스트리밍 소프트웨어에 스트림 키 입력"
                    ),
                    List.of(
                            "스튜디오 > 방송하기로 이동해서 스트림 키부터 확인해 보세요.",
                            "OBS 설정 중이면 스트림 키가 맞게 들어갔는지도 같이 확인해 주세요."
                    )
            );
        }

        if (containsAny(text, "알림")) {
            return """
                    방송 알림 관련 설정은 스튜디오 > 알림 메뉴에서 관리하는 흐름이에요.

                    다음 행동:
                    - 방송 알림인지 후원 알림인지 같이 알려 주시면 더 좁혀드릴게요.
                    """;
        }

        if (containsAny(text, "채팅설정", "금칙어", "클린봇", "저속모드", "이모티콘모드")) {
            return """
                    스튜디오 채팅 설정에서는 클린봇, 채팅 권한, 이모티콘 모드, 저속 모드 같은 항목을 보는 흐름이 있어요.

                    다음 행동:
                    - 스튜디오 > 채팅 설정으로 이동해 보세요.
                    - 찾는 항목이 금칙어인지 권한인지 알려 주시면 더 정확히 설명할게요.
                    """;
        }

        if (containsAny(text, "수익", "매출", "후원내역")) {
            return """
                    방송 수익이나 후원 관련 확인은 스튜디오 > 수익 메뉴와 프로필 결제 메뉴를 같이 보는 경우가 많아요.

                    다음 행동:
                    - 방송 기준 수익이면 스튜디오 > 수익
                    - 개인 결제/잔액 기준이면 프로필 > 팡 또는 구독
                    """;
        }

        if (containsAny(text, "시청자차단", "차단목록", "blocklist")) {
            return """
                    시청자 차단 목록은 스튜디오 안의 시청자 관리 흐름에서 확인하는 구조예요.

                    다음 행동:
                    - 스튜디오 > 시청자 > 차단 목록 메뉴를 확인해 보세요.
                    """;
        }

        return """
                스튜디오에서는 방송 시작, 설정, 알림, 채팅 설정, 분석, 시청자 관리, 수익 확인까지 이어서 볼 수 있어요.

                다음 행동:
                - 방송 시작이면 스튜디오 > 방송하기
                - 채팅 관련이면 스튜디오 > 채팅 설정
                - 알림이면 스튜디오 > 알림
                - 수익이면 스튜디오 > 수익
                """;
    }

    private boolean isIdentityQuestion(String text) {
        return containsAny(
                text,
                "넌뭐야", "너뭐야", "너뭔데", "넌뭔데", "넌누구야", "너는누구야", "누구야", "누구세요",
                "정체", "뭐하는애", "뭐하는챗봇", "무슨챗봇", "어떤챗봇", "ai야", "사람이야", "뭐하는거야", "뭐해주는애야"
        );
    }

    private boolean isServiceQuestion(String text) {
        return containsAny(
                text,
                "이건무슨채팅", "이채팅뭐야", "무슨채팅", "어떤채팅", "뭐하는채팅", "무슨서비스", "여기뭐하는곳"
        );
    }

    private boolean isOutOfScopeConversation(String text) {
        if (containsAny(
                text,
                "바보", "멍청", "병신", "찌질", "재미없", "심심", "놀아줘", "잡담", "수다", "욕해", "욕해봐",
                "날씨", "주식", "비트코인", "축구", "야구", "농구", "영화", "드라마", "연예인", "운세", "사주",
                "코딩문제", "숙제", "번역", "시써줘", "노래가사", "아재개그"
        )) {
            return true;
        }

        return !isLikelyInScope(text);
    }

    private boolean isLikelyInScope(String text) {
        return isIdentityQuestion(text)
                || isServiceQuestion(text)
                || containsAny(
                text,
                "전적", "검색", "기록", "롤", "lol", "tft", "발로", "valorant", "배그", "pubg", "오버워치", "overwatch", "cs2",
                "계정", "연동", "디스코드", "discord", "스팀", "steam", "블리자드", "라이엇", "riot",
                "프로필", "내정보", "마이페이지", "닉네임", "소개",
                "커뮤니티", "게시글", "글쓰기", "댓글", "답글", "좋아요", "북마크",
                "채팅", "dm", "친구", "알림", "단체채팅", "랜덤채팅", "게임방", "매칭",
                "신고", "차단", "후원", "결제", "팡", "마일리지", "광고제거", "구독",
                "방송", "스트림", "스튜디오", "obs", "수익", "오류", "문제", "로그인", "회원가입"
        );
    }

    private String defaultFallback(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return """
                    필요한 기능을 보내 주시면 바로 정리해 드릴게요.

                    👉 다음 행동
                    - 전적 검색인지
                    - 계정 연동인지
                    - 채팅, 후원, 스튜디오인지
                    하나만 먼저 알려 주세요.
                    """;
        }

        return """
                필요한 작업 기준으로 바로 안내해 드릴게요.

                👉 다음 행동
                - 어떤 기능에서 막혔는지 한 줄로 적어 주세요.
                - 가능하면 메뉴 이름도 같이 적어 주세요. 예: 전적검색, 계정 연동, DM, 스튜디오
                - 화면 이름이나 오류 문구가 있으면 함께 보내 주세요.
                """;
    }

    private String buildSystemPrompt() {
        return """
                You are GM Mate, the in-app GameMatcher assistant.
                Reply in Korean unless the user explicitly asks for another language.
                Sound like a practical service guide, not a generic AI assistant.
                You have a stable identity: you are the GameMatcher product helper that explains flows, input formats, and troubleshooting.
                Handle identity questions like "Who are you?" and service questions like "What is this chat?" naturally.
                If the user talks outside the implemented website features or starts pointless chatter, reply only with: "도와드릴 내용이 있으면 말씀해 주세요."
                Prefer this response style:
                1. Answer the question directly.
                2. Keep the first paragraph to one or two short sentences.
                3. Explain only the exact page, menu, or input format that matters.
                4. Use clean Korean formatting with short sections and light emoji headings like 📌, ✅, 👉 when helpful.
                5. Avoid long introductions, repetition, and more than three bullets unless necessary.
                Be concise, grounded, action-oriented, and product-specific.
                Do not invent unsupported features, hidden integrations, or real-world actions you cannot perform.
                Avoid generic AI disclaimers and avoid sounding robotic.
                When relevant, mention concrete paths like:
                - 전적검색 페이지
                - 프로필 > 내 정보
                - 프로필 > 외부 계정 연동
                - 커뮤니티 > 글쓰기
                - DM / 단체 채팅 / 게임방
                - 스튜디오 > 방송하기 / 알림 / 채팅 설정 / 수익
                If the user asks about an error, focus on the most likely checks first.
                """;
    }

    private String buildFeatureGuide() {
        return """
                GameMatcher feature guide:
                - Users can search game records for LoL, TFT, Valorant, PUBG, Overwatch 2, and CS2.
                - LoL and TFT use game name plus tag.
                - Valorant uses player name plus tag, and region may matter.
                - PUBG uses nickname plus platform selection such as Steam or Kakao.
                - Overwatch 2 uses BattleTag name plus numeric tag.
                - CS2 uses Steam64 ID or Steam vanity URL.
                - Users can manage profile information in profile home and profile > my-info.
                - External account linking exists in profile > account-links for Discord, Steam, Blizzard, and Riot.
                - Riot linking is handled by entering game name and tag directly.
                - Community features include post list, board tabs, post detail, writing, editing, comments, replies, likes, bookmarks, and reports.
                - Chat features include direct messages with friends, group chat, floating chat widget, and random match chat.
                - Game room / matching flow exists for creating rooms, joining rooms, and entering connected group chat rooms.
                - Safety features include report and block flows.
                - Payment-related features include pang balance, pang charge and refund, mileage, subscriptions, and ad-free purchase.
                - Studio features include dashboard, live streaming, settings, alerts, chat settings, analysis, viewers, block list, and revenue.
                - Login may be required for profile, account-link, posting, direct messages, payments, and studio features.
                - Do not answer weather, finance, sports, entertainment, coding, homework, or generic chatting topics.
                - If a message is insulting, idle banter, or unrelated to these product features, reply with exactly: 도와드릴 내용이 있으면 말씀해 주세요.
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

    private String formatGuide(String summary, List<String> steps, List<String> nextActions) {
        StringBuilder builder = new StringBuilder();
        builder.append(summary).append("\n\n");

        if (steps != null && !steps.isEmpty()) {
            builder.append("📌 바로 보면 되는 내용\n");
            for (String step : steps) {
                builder.append("- ").append(step).append("\n");
            }
            builder.append("\n");
        }

        if (nextActions != null && !nextActions.isEmpty()) {
            builder.append("👉 다음 행동\n");
            for (String action : nextActions) {
                builder.append("- ").append(action).append("\n");
            }
        }

        return builder.toString().trim();
    }

    private String sanitizeReply(String reply) {
        return reply
                .replace("\r\n", "\n")
                .replace("\n>", "\n👉 ")
                .replace("바로 보면 되는 내용:", "📌 바로 보면 되는 내용")
                .replace("도와드릴 수 있는 범위:", "📌 도와드릴 수 있는 범위")
                .replace("체크 포인트:", "✅ 체크 포인트")
                .replace("다음 행동:", "👉 다음 행동")
                .replace("가능한 기능:", "📌 가능한 기능")
                .replace("가능한 흐름:", "📌 가능한 흐름")
                .replace("흐름:", "📌 흐름")
                .replace("참고:", "💡 참고")
                .trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("\n", "")
                .replace("\t", "")
                .replace("-", "")
                .replace("_", "")
                .replace("#", "")
                .replace("?", "")
                .replace("!", "")
                .replace(".", "")
                .replace(",", "")
                .replace("~", "");
    }
}
