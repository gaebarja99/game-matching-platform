package com.gamematcher.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.text.Normalizer;

/**
 * 전적 검색 요청 DTO
 *
 * [Riot 계열 (lol, tft, valorant)]
 * - gameName: "hide on bush"  (# 앞부분)
 * - tagLine:  "KR1"           (# 뒷부분)
 * - 또는 nickname: "hide on bush#KR1" 형식도 지원 (자동 파싱)
 *
 * [Steam]
 * - steamId: Steam64 ID 또는 커스텀 URL
 *
 * [Blizzard]
 * - gameName: BattleTag 앞부분
 * - tagLine:  BattleTag 뒷 숫자
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerSearchRequest {

    /**
     * 게임 종류: lol | tft | valorant | steam | blizzard
     */
    private String game;

    /**
     * 닉네임 (# 앞부분)
     * - LoL/TFT/Valorant: 게임 이름 (e.g., "hide on bush")
     * - Blizzard: BattleTag 앞부분 (e.g., "Player")
     */
    private String gameName;

    /**
     * 태그라인 (# 뒷부분)
     * - LoL/TFT/Valorant: 태그 (e.g., "KR1")
     * - Blizzard: 배틀태그 번호 (e.g., "1234")
     */
    private String tagLine;

    /**
     * "gameName#tagLine" 형식으로 한번에 입력 시 자동 파싱 지원
     * 예: "hide on bush#KR1"
     */
    private String nickname;

    /**
     * Steam 전용 - Steam64 ID
     */
    private String steamId;

    /**
     * 플랫폼
     * - PUBG: steam | kakao | xbox | psn
     * - 기본값: steam
     */
    private String platform = "steam";

    /**
     * 지역 (기본값: kr)
     * - Riot: asia, americas, europe, kr, jp, ...
     */
    private String region = "kr";

    /**
     * 조회할 매치 수 (기본값: 10, 최대: 20)
     */
    private Integer count = 10;

    /**
     * LoL 큐 타입 필터 (선택)
     * 420: 솔로랭크, 440: 자유랭크, 450: ARAM, 400: 일반, null: 전체
     */
    private Integer queueType;

    /**
     * true 이면 DB에 캐시된 매치가 있어도 외부 API로 다시 받아 갱신한다.
     */
    private Boolean forceRefresh;

    /**
     * true 이면 매치 ID 목록까지만 조회하고, 매치 상세(match-v5 등)는 호출하지 않는다.
     * 상세는 {@code POST /api/search/match-detail} 로 지연 로드한다.
     */
    private Boolean matchListOnly;

    /**
     * true 이면 발로란트 전적 검색에서 MMR(티어) 조회를 생략하고, {@code POST /api/search/valorant/mmr} 로 이어서 받는다.
     */
    private Boolean deferValorantMmr;

    /**
     * true 이면 계정(소환사/발로 Riot ID) 존재 여부만 확인하고 매치·랭크 등은 조회하지 않는다.
     * 발로란트: 1차 프로브 후 전체 전적은 {@code valorantPrefetchPuuid} 등으로 이어서 호출.
     */
    private Boolean accountOnly;

    /** 발로란트: 계정 프로브 직후 전체 검색 시 계정 API를 다시 호출하지 않도록 전달 */
    private String valorantPrefetchPuuid;
    /** Henrik 계정 API의 {@code region} 원문(예: ap). 없으면 요청 region 기준 폴백 */
    private String valorantPrefetchAccountRegion;
    private String valorantPrefetchCardUrl;

    /**
     * nickname 필드에서 gameName / tagLine 자동 파싱
     */
    public void parseNickname() {
        if (nickname != null && !nickname.isEmpty()) {
            int idx = nickname.lastIndexOf('#');
            if (idx > 0) {
                this.gameName = nickname.substring(0, idx).trim();
                this.tagLine  = nickname.substring(idx + 1).trim();
            } else {
                this.gameName = nickname.trim();
            }
        }
    }

    /**
     * 유효성 검사 후 파싱까지 수행
     */
    public PlayerSearchRequest normalize() {
        parseNickname();
        if (region == null || region.isEmpty()) region = "kr";
        // JSON에 count 생략 시 null → NPE 방지 (PUBG·APEX 등 count 필드 없는 전적 화면)
        if (count == null || count <= 0 || count > 20) count = 10;
        if (nickname != null) nickname = normalizeText(nickname);
        if (gameName != null) gameName = normalizeText(gameName);
        if (tagLine != null) tagLine = normalizeText(tagLine);
        if (game != null) game = game.toLowerCase().trim();
        return this;
    }

    private String normalizeText(String value) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return trimmed;
        }
        return Normalizer.normalize(trimmed, Normalizer.Form.NFC);
    }
}
