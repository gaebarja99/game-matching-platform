package com.gamematcher.service.overwatch;

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
 * ow-api.com 비공식 API 사용 (API 키 불필요)
 * https://ow-api.com/docs/
 *
 * 플레이어 프로필이 '공개(public)' 설정이어야 데이터 조회 가능
 *
 * 닉네임 형식:
 *   gameName = "Fleta"   (BattleTag 앞부분)
 *   tagLine  = "3852"    (BattleTag 숫자)
 *   → Fleta#3852
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverwatchApiService {

    private final RestTemplate restTemplate;

    private static final String OW_API_BASE = "https://ow-api.com/v1/stats";

    private static final Map<String, String> PLATFORM_MAP = Map.of(
            "pc", "pc", "kr", "pc",
            "psn", "psn", "ps4", "psn",
            "xbox", "xbl", "xbl", "xbl"
    );

    private static final Map<String, String> TIER_DISPLAY = new LinkedHashMap<>();
    static {
        TIER_DISPLAY.put("bronze",      "🟫 브론즈");
        TIER_DISPLAY.put("silver",      "⬜ 실버");
        TIER_DISPLAY.put("gold",        "🟨 골드");
        TIER_DISPLAY.put("platinum",    "🩵 플래티넘");
        TIER_DISPLAY.put("diamond",     "💎 다이아몬드");
        TIER_DISPLAY.put("master",      "🔮 마스터");
        TIER_DISPLAY.put("grandmaster", "🔶 그랜드마스터");
        TIER_DISPLAY.put("top500",      "🏆 탑500");
    }

    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String gameName = req.getGameName();
        String tagLine  = req.getTagLine();
        String nickname = tagLine != null ? gameName + "#" + tagLine : gameName;
        String platform = PLATFORM_MAP.getOrDefault(
                req.getRegion() != null ? req.getRegion().toLowerCase() : "pc", "pc"
        );

        if (gameName == null || gameName.isBlank()) {
            return PlayerSearchResponse.error("overwatch", nickname, "닉네임을 입력해주세요.");
        }
        if (tagLine == null || tagLine.isBlank()) {
            return PlayerSearchResponse.error("overwatch", nickname,
                    "BattleTag 번호를 입력해주세요. 예: Fleta#3852 → tagLine=3852");
        }

        try {
            // BattleTag의 # → - 로 변환 (ow-api URL 규칙)
            String battleTagEncoded = gameName + "-" + tagLine;
            String profileUrl = String.format(
                    "%s/%s/global/%s/profile",
                    OW_API_BASE, platform, battleTagEncoded
            );
            log.info("오버워치 API 호출: {}", profileUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "GameMatcher/1.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> respEntity = restTemplate.exchange(
                    profileUrl, HttpMethod.GET, entity, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = respEntity.getBody();

            if (profile == null) {
                return PlayerSearchResponse.error("overwatch", nickname, "프로필을 찾을 수 없습니다.");
            }
            if (Boolean.TRUE.equals(profile.get("private"))) {
                return PlayerSearchResponse.error("overwatch", nickname,
                        "비공개 프로필입니다.\n오버워치2 내 설정 → 소셜 → 프로필 → '모두에게 공개'로 변경 후 재시도하세요.");
            }

            String playerName = (String) profile.getOrDefault("name", nickname);
            String avatarUrl  = (String) profile.get("icon");
            int    level      = toInt(profile.get("level"));
            int    prestige   = toInt(profile.get("prestige"));

            // 경쟁전 랭크 파싱
            String competitiveTier = "UNRANKED";
            @SuppressWarnings("unchecked")
            Map<String, Object> competitive = (Map<String, Object>) profile.get("competitive");
            if (competitive != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pc = (Map<String, Object>) competitive.get("pc");
                if (pc != null) {
                    competitiveTier = getBestTier(
                            (String) pc.get("tank"),
                            (String) pc.get("damage"),
                            (String) pc.get("support")
                    );
                }
            }

            // 영웅별 플레이 시간 → MatchInfo 변환
            @SuppressWarnings("unchecked")
            Map<String, Object> qpStats   = (Map<String, Object>) profile.get("quickPlayStats");
            @SuppressWarnings("unchecked")
            Map<String, Object> compStats = (Map<String, Object>) profile.get("competitiveStats");

            List<MatchInfo> matches = buildHeroMatches(qpStats);
            MatchStats stats        = buildStats(qpStats);

            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("level",       prestige * 100 + level);
            rawData.put("gamesPlayed", extractStat(qpStats, "game", "gamesPlayed"));
            rawData.put("gamesWon",    extractStat(qpStats, "game", "gamesWon"));

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .gameName(playerName)
                    .tagLine(tagLine)
                    .summonerLevel(String.valueOf(prestige * 100 + level))
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
                msg = "플레이어를 찾을 수 없습니다: " + nickname
                        + "\n· BattleTag 정확히 입력했는지 확인 (예: Fleta#3852)"
                        + "\n· 플랫폼 확인 (pc / psn / xbox)";
            } else if (msg != null && msg.contains("503")) {
                msg = "ow-api.com 서버가 일시 응답 불가입니다. 잠시 후 재시도하세요.";
            }
            return PlayerSearchResponse.error("overwatch", nickname, msg);
        }
    }

    @SuppressWarnings("unchecked")
    private List<MatchInfo> buildHeroMatches(Map<String, Object> qpStats) {
        if (qpStats == null) return List.of();
        Map<String, Object> careerStats = (Map<String, Object>) qpStats.get("careerStats");
        if (careerStats == null) return List.of();

        List<MatchInfo> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : careerStats.entrySet()) {
            String heroKey = entry.getKey();
            if ("allHeroes".equals(heroKey)) continue;
            if (!(entry.getValue() instanceof Map)) continue;

            Map<String, Object> heroData = (Map<String, Object>) entry.getValue();
            Map<String, Object> gameStats   = (Map<String, Object>) heroData.get("game");
            Map<String, Object> combatStats = (Map<String, Object>) heroData.get("combat");

            String playtimeStr = gameStats != null
                    ? (String) gameStats.getOrDefault("timePlayed", "0") : "0";
            int playtimeSec = parseTime(playtimeStr);
            if (playtimeSec == 0) continue;

            int kills  = combatStats != null ? toInt(combatStats.get("eliminations")) : 0;
            int deaths = combatStats != null ? toInt(combatStats.get("deaths")) : 0;
            int wins   = gameStats   != null ? toInt(gameStats.get("gamesWon")) : 0;
            double kda = deaths > 0 ? (double) kills / deaths : kills;

            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("hero",        formatHeroName(heroKey));
            extras.put("timePlayed",  playtimeStr);
            extras.put("wins",        wins + "승");
            if (combatStats != null) {
                extras.put("damageDone",  combatStats.get("heroDamageDone"));
                extras.put("healingDone", combatStats.get("healingDone"));
                extras.put("accuracy",    combatStats.get("weaponAccuracy"));
            }

            result.add(MatchInfo.builder()
                    .matchId(heroKey)
                    .gameMode("일반 · " + formatHeroName(heroKey))
                    .agent(formatHeroName(heroKey))
                    .kills(kills).deaths(deaths).assists(0)
                    .kda(Math.round(kda * 100.0) / 100.0)
                    .playtime(playtimeSec)
                    .extras(extras)
                    .build());
        }

        result.sort((a, b) -> Integer.compare(
                b.getPlaytime() != null ? b.getPlaytime() : 0,
                a.getPlaytime() != null ? a.getPlaytime() : 0));
        return result.stream().limit(10).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private MatchStats buildStats(Map<String, Object> qpStats) {
        int totalGames = 0, wins = 0;
        double avgKda  = 0;
        if (qpStats != null) {
            Map<String, Object> cs = (Map<String, Object>) qpStats.get("careerStats");
            Map<String, Object> ah = cs != null ? (Map<String, Object>) cs.get("allHeroes") : null;
            if (ah != null) {
                Map<String, Object> game   = (Map<String, Object>) ah.get("game");
                Map<String, Object> combat = (Map<String, Object>) ah.get("combat");
                if (game != null) {
                    totalGames = toInt(game.get("gamesPlayed"));
                    wins       = toInt(game.get("gamesWon"));
                }
                if (combat != null) {
                    int elims  = toInt(combat.get("eliminations"));
                    int deaths = toInt(combat.get("deaths"));
                    avgKda = deaths > 0 ? (double) elims / deaths : elims;
                }
            }
        }
        double winRate = totalGames > 0
                ? Math.round((double) wins / totalGames * 1000.0) / 10.0 : 0;
        return MatchStats.builder()
                .totalGames(totalGames).wins(wins).losses(Math.max(0, totalGames - wins))
                .winRate(winRate).avgKda(Math.round(avgKda * 100.0) / 100.0)
                .build();
    }

    private String getBestTier(String... roles) {
        String[] order = {"top500","grandmaster","master","diamond","platinum","gold","silver","bronze"};
        for (String tier : order)
            for (String role : roles)
                if (tier.equalsIgnoreCase(role))
                    return TIER_DISPLAY.getOrDefault(tier, tier.toUpperCase());
        return "UNRANKED";
    }

    private String formatHeroName(String key) {
        if (key == null) return "";
        Map<String, String> names = Map.of(
                "soldier76","솔저: 76","doomfist","둠피스트",
                "junkerqueen","정커 퀸","wreckinball","레킹볼",
                "reinhardt","라인하르트","widowmaker","위도우메이커",
                "cassidy","캐시디","baptiste","바티스트",
                "lucio","루시우","moira","모이라"
        );
        return names.getOrDefault(key.toLowerCase(),
                key.substring(0,1).toUpperCase() + key.substring(1));
    }

    private int parseTime(String t) {
        if (t == null || t.isEmpty()) return 0;
        try {
            String[] p = t.split(":");
            if (p.length == 3) return Integer.parseInt(p[0])*3600 + Integer.parseInt(p[1])*60 + Integer.parseInt(p[2]);
            if (p.length == 2) return Integer.parseInt(p[0])*60  + Integer.parseInt(p[1]);
        } catch (Exception ignored) {}
        return 0;
    }

    @SuppressWarnings("unchecked")
    private String extractStat(Map<String,Object> stats, String category, String key) {
        if (stats == null) return "0";
        try {
            Map<String,Object> cs = (Map<String,Object>) stats.get("careerStats");
            Map<String,Object> ah = cs != null ? (Map<String,Object>) cs.get("allHeroes") : null;
            Map<String,Object> cat = ah != null ? (Map<String,Object>) ah.get(category) : null;
            return cat != null ? String.valueOf(cat.getOrDefault(key, 0)) : "0";
        } catch (Exception e) { return "0"; }
    }

    private int toInt(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s,"UTF-8"); } catch (Exception e) { return s; }
    }
}

