package com.gamematcher.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * 조회할 매치 수 (기본값: 5, 최대: 20)
     */
    private Integer count = 5;

    /**
     * LoL 큐 타입 필터 (선택)
     * 420: 솔로랭크, 440: 자유랭크, 450: ARAM, 400: 일반, null: 전체
     */
    private Integer queueType;

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
        if (count <= 0 || count > 20) count = 5;
        if (game != null) game = game.toLowerCase().trim();
        return this;
    }
}
