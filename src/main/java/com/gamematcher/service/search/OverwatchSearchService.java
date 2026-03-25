package com.gamematcher.service.search;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 오버워치 2 전적 검색 서비스
 *
 * OverFast API 사용 (https://overfast-api.tekrop.fr)
 * API 키 불필요 - 공개 API
 *
 * 닉네임 형식:
 *   gameName = "Fleta"   (BattleTag 앞부분)
 *   tagLine  = "3852"    (BattleTag 숫자)
 *   => 플레이어 ID: Fleta-3852
 *
 * 주의: 플레이어 프로필이 공개 설정이어야 통계 조회 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverwatchSearchService {

    private final RestTemplate restTemplate;

    private static final String OW_API_BASE = "https://overfast-api.tekrop.fr";

    private static final Map<String, String> TIER_DISPLAY = new LinkedHashMap<>();
    static {
        TIER_DISPLAY.put("BRONZE",       "🟫 브론즈");
        TIER_DISPLAY.put("SILVER",       "⬜ 실버");
        TIER_DISPLAY.put("GOLD",         "🟨 골드");
        TIER_DISPLAY.put("PLATINUM",     "🩵 플래티넘");
        TIER_DISPLAY.put("DIAMOND",      "💎 다이아몬드");
        TIER_DISPLAY.put("MASTER",       "🔮 마스터");
        TIER_DISPLAY.put("GRANDMASTER",  "🔶 그랜드마스터");
        TIER_DISPLAY.put("CHAMPION",     "🏆 챔피언");
    }

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String gameName = req.getGameName();
        String tagLine  = req.getTagLine();
        String nickname = tagLine != null && !tagLine.isBlank()
                ? gameName + "#" + tagLine : gameName;

        if (gameName == null || gameName.isBlank()) {
            return PlayerSearchResponse.error("overwatch", nickname, "닉네임을 입력해주세요.");
        }
        if (tagLine == null || tagLine.isBlank()) {
            return PlayerSearchResponse.error("overwatch", nickname,
                    "BattleTag 번호를 입력해주세요. 예: Fleta#3852 -> tagLine=3852");
        }

        try {
            // OverFast API: BattleTag의 # -> - 로 변환
            String playerId = gameName + "-" + tagLine;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "GameMatcher/1.0");
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 1) 프로필 요약 조회
            String summaryUrl = OW_API_BASE + "/players/" + playerId + "/summary";
            log.info("오버워치 API 호출: {}", summaryUrl);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> summaryResp = restTemplate.exchange(
                    summaryUrl, HttpMethod.GET, entity, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = summaryResp.getBody();

            if (summary == null) {
                return PlayerSearchResponse.error("overwatch", nickname, "프로필을 찾을 수 없습니다.");
            }

            String playerName = (String) summary.getOrDefault("username", nickname);
            String avatarUrl  = summary.get("avatar") instanceof String s ? s : null;

            // 경쟁전 랭크 파싱
            String competitiveTier = "UNRANKED";
            @SuppressWarnings("unchecked")
            Map<String, Object> competitive = (Map<String, Object>) summary.get("competitive");
            if (competitive != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pc = (Map<String, Object>) competitive.get("pc");
                if (pc != null) {
                    Object rankObj = pc.get("rank_tier");
                    String rank = rankObj instanceof String s ? s.toUpperCase() : null;
                    if (rank != null) {
                        competitiveTier = TIER_DISPLAY.getOrDefault(rank, rank);
                    }
                }
            }

            // 2) 통계 조회
            String statsUrl = OW_API_BASE + "/players/" + playerId + "/stats/summary";
            List<MatchInfo> matches = List.of();
            MatchStats stats = MatchStats.builder().build();
            int totalGames = 0, wins = 0;
            double avgKda = 0;

            try {
                @SuppressWarnings("unchecked")
                ResponseEntity<Map> statsResp = restTemplate.exchange(
                        statsUrl, HttpMethod.GET, entity, Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> statsBody = statsResp.getBody();
                if (statsBody != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> general = (Map<String, Object>) statsBody.get("general");
                    if (general != null) {
                        totalGames = toInt(general.get("games_played"));
                        wins       = toInt(general.get("games_won"));
                        int elims  = toInt(general.get("eliminations"));
                        int deaths = toInt(general.get("deaths"));
                        avgKda = deaths > 0 ? Math.round((double) elims / deaths * 100.0) / 100.0 : elims;
                    }
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> heroStats =
                            (List<Map<String, Object>>) statsBody.get("heroes");
                    if (heroStats != null) {
                        matches = buildHeroMatches(heroStats);
                    }
                }
            } catch (Exception ex) {
                log.warn("오버워치 통계 조회 실패 (비공개일 수 있음): {}", ex.getMessage());
            }

            double winRate = totalGames > 0
                    ? Math.round((double) wins / totalGames * 1000.0) / 10.0 : 0;
            stats = MatchStats.builder()
                    .totalGames(totalGames).wins(wins).losses(Math.max(0, totalGames - wins))
                    .winRate(winRate).avgKda(avgKda)
                    .build();

            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("gamesPlayed", totalGames);
            rawData.put("gamesWon", wins);
            rawData.put("winRate", winRate + "%");

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .gameName(playerName)
                    .tagLine(tagLine)
                    .tier(competitiveTier)
                    .avatarUrl(avatarUrl)
                    .rawData(rawData)
                    .build();

            return PlayerSearchResponse.builder()
                    .success(true)
                    .game("overwatch")
                    .nickname(playerName)
                    .playerInfo(playerInfo)
                    .matches(matches)
                    .stats(stats)
                    .build();

        } catch (Exception e) {
            log.error("오버워치 전적 검색 오류 - {}", nickname, e);
            String msg = e.getMessage();
            if (msg != null && msg.contains("404")) {
                // OverFast API는 첫 조회 시 데이터 수집 후 retry_after 응답을 줌
                if (msg.contains("retry_after")) {
                    msg = "플레이어 데이터를 수집 중입니다: " + nickname
                            + "\n· OverFast API가 처음 요청된 플레이어 데이터를 수집하고 있습니다."
                            + "\n· 약 10분 후 다시 시도하면 정상 조회됩니다."
                            + "\n· BattleTag 형식 확인: 예) Fleta#3852";
                } else {
                    msg = "플레이어를 찾을 수 없습니다: " + nickname
                            + "\n· BattleTag 정확히 입력했는지 확인 (예: Fleta#3852)"
                            + "\n· 프로필이 공개로 설정되어 있는지 확인하세요";
                }
            } else if (msg != null && (msg.contains("503") || msg.contains("502"))) {
                msg = "오버워치 API 서버가 일시 응답 불가입니다. 잠시 후 재시도하세요.";
            }
            return PlayerSearchResponse.error("overwatch", nickname, msg);
        }
    }

    private List<MatchInfo> buildHeroMatches(List<Map<String, Object>> heroStats) {
        List<MatchInfo> result = new ArrayList<>();
        for (Map<String, Object> hero : heroStats) {
            String heroName = (String) hero.getOrDefault("hero", "");
            int timeSec  = toInt(hero.get("time_played"));
            int elims    = toInt(hero.get("eliminations"));
            int deaths   = toInt(hero.get("deaths"));
            int wins     = toInt(hero.get("games_won"));
            if (timeSec == 0) continue;

            double kda = deaths > 0 ? (double) elims / deaths : elims;
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("hero",       heroName);
            extras.put("timePlayed", formatSeconds(timeSec));
            extras.put("wins",       wins + "승");
            extras.put("elims",      elims);
            extras.put("deaths",     deaths);

            result.add(MatchInfo.builder()
                    .matchId(heroName.toLowerCase())
                    .gameMode("오버워치 · " + heroName)
                    .agent(heroName)
                    .kills(elims).deaths(deaths).assists(0)
                    .kda(Math.round(kda * 100.0) / 100.0)
                    .playtime(timeSec)
                    .extras(extras)
                    .build());
        }
        result.sort((a, b) -> Integer.compare(
                b.getPlaytime() != null ? b.getPlaytime() : 0,
                a.getPlaytime() != null ? a.getPlaytime() : 0));
        return result.stream().limit(10).collect(Collectors.toList());
    }

    private String formatSeconds(int sec) {
        int h = sec / 3600, m = (sec % 3600) / 60;
        if (h > 0) return h + "시간 " + m + "분";
        return m + "분";
    }

    private int toInt(Object o) { return o instanceof Number n ? n.intValue() : 0; }
}