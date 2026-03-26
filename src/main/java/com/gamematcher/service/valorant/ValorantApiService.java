package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.ValorantApiProperties;
import com.gamematcher.dto.valorant.ValorantLifetimeApiResponse;
import com.gamematcher.dto.valorant.ValorantLifetimeDataItem;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.dto.valorant.ValorantMmrHistoryApiResponse;
import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import com.gamematcher.service.MatchApiCachePolicy;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import com.gamematcher.dto.search.ValorantSearchMmrRequest;
import com.gamematcher.dto.search.ValorantSearchMmrResponse;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;
import com.gamematcher.repository.match.ValorantMatchDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Valorant 전적 조회 (HenrikDev API) 및 DB 저장
 * API 상세: 발로란트 api 목록 문서 기반
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValorantApiService {

    private final ValorantAccountService valorantAccountService;

    private static final Map<String, String> REGION_MAP = new HashMap<>();
    static {
        REGION_MAP.put("kr", "kr"); REGION_MAP.put("kr1", "kr");
        REGION_MAP.put("ap", "ap"); REGION_MAP.put("asia", "ap"); REGION_MAP.put("jp", "ap"); REGION_MAP.put("jp1", "ap"); REGION_MAP.put("sg", "ap");
        REGION_MAP.put("na", "na"); REGION_MAP.put("na1", "na");
        REGION_MAP.put("eu", "eu"); REGION_MAP.put("euw", "eu"); REGION_MAP.put("euw1", "eu"); REGION_MAP.put("eune1", "eu");
        REGION_MAP.put("latam", "latam"); REGION_MAP.put("la1", "latam"); REGION_MAP.put("la2", "latam");
        REGION_MAP.put("br", "br"); REGION_MAP.put("br1", "br");
    }

    private final ValorantApiProperties valorantApiProperties;
    private final ValorantMatchService valorantMatchService;
    private final ValorantMatchJsonService valorantMatchJsonService;
    private final ValorantMatchDetailRepository valorantMatchDetailRepository;
    private final ValorantMatchResultService valorantMatchResultService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 전적 검색 시 Henrik MMR / lifetime 응답을 잠깐 보관해 재검색·새로고침 시 외부 지연을 줄인다.
     * MMR과 lifetime은 서로 독립이므로 캐시 미스 시 {@link ForkJoinPool#commonPool()} 로 병렬 호출한다.
     */
    private static final long VALORANT_SEARCH_API_CACHE_TTL_MS = Duration.ofMinutes(3).toMillis();

    private static final Set<String> HENRIK_SHARDS = Set.of("ap", "na", "eu", "kr", "br", "latam");

    private final ConcurrentHashMap<String, Cached<ValorantMmrTierSnapshot>> valorantMmrSearchCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cached<List<MatchInfo>>> valorantLifetimeSearchCache = new ConcurrentHashMap<>();

    private record Cached<T>(T value, long expiresAtMs) {
        boolean fresh() {
            return System.currentTimeMillis() < expiresAtMs;
        }
    }

    /**
     * API URL에 api_key 쿼리 파라미터 추가 (설정된 경우)
     */
    private String appendApiKey(String url) {
        if (!valorantApiProperties.hasApiKey()) {
            return url;
        }
        return UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api_key", valorantApiProperties.getApiKey())
                .build()
                .toUriString();
    }

    private HttpEntity<Void> valorantHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (valorantApiProperties.hasApiKey()) {
            headers.set("Authorization", valorantApiProperties.getApiKey().trim());
        }
        return new HttpEntity<>(headers);
    }

    /**
     * 계정 API의 region 문자열(또는 요청값)을 Henrik MMR/매치 URL용 샤드로 변환.
     */
    private String resolveShardRegion(PlayerSearchRequest req, String accountApiRegion) {
        String raw;
        if (accountApiRegion != null && !accountApiRegion.isBlank()) {
            raw = accountApiRegion.toLowerCase().trim();
        } else if (req.getRegion() != null && !req.getRegion().isBlank()) {
            raw = req.getRegion().toLowerCase().trim();
        } else {
            raw = "kr";
        }
        return REGION_MAP.getOrDefault(raw, "ap");
    }

    /** Henrik MMR/매치 URL용 샤드 (ap, na, eu, kr, br, latam) */
    public String mapAccountRegionToHenrikShard(String valorantAccountRegion) {
        if (valorantAccountRegion == null || valorantAccountRegion.isBlank()) {
            return "ap";
        }
        return REGION_MAP.getOrDefault(valorantAccountRegion.toLowerCase().trim(), "ap");
    }

    /** 클라이언트가 넘긴 region 문자열을 Henrik 샤드로 정규화 */
    public String normalizeHenrikShard(String region) {
        if (region == null || region.isBlank()) {
            return "ap";
        }
        String raw = region.toLowerCase(Locale.ROOT).trim();
        if (HENRIK_SHARDS.contains(raw)) {
            return raw;
        }
        return REGION_MAP.getOrDefault(raw, "ap");
    }

    /**
     * Records용: 1차 검색({@code deferValorantMmr}) 이후 티어만 조회.
     */
    public ValorantSearchMmrResponse resolveMmrForSearch(ValorantSearchMmrRequest r) {
        String puuid = r.getPuuid();
        if (puuid == null || puuid.isBlank()) {
            return ValorantSearchMmrResponse.fail("puuid가 필요합니다.");
        }
        String shard = normalizeHenrikShard(r.getRegion());
        String mmrCacheKey = shard + "|" + puuid.trim();
        if (Boolean.TRUE.equals(r.getForceRefresh())) {
            valorantMmrSearchCache.remove(mmrCacheKey);
        }
        HttpEntity<Void> entity = valorantHttpEntity();
        Cached<ValorantMmrTierSnapshot> cached = valorantMmrSearchCache.get(mmrCacheKey);
        ValorantMmrTierSnapshot snap;
        if (cached != null && cached.fresh()) {
            snap = cached.value();
        } else {
            snap = fetchMmrAndCache(mmrCacheKey, shard, puuid.trim(), entity);
        }
        return ValorantSearchMmrResponse.ok(snap.displayTier());
    }

    private ValorantMmrTierSnapshot fetchMmrAndCache(String mmrCacheKey, String region, String puuid,
                                                     HttpEntity<Void> entity) {
        ValorantMmrTierSnapshot m = fetchValorantMmrForSearch(region, puuid, entity);
        valorantMmrSearchCache.put(mmrCacheKey,
                new Cached<>(m, System.currentTimeMillis() + VALORANT_SEARCH_API_CACHE_TTL_MS));
        return m;
    }

    private List<MatchInfo> fetchMatchesAndCache(String matchCacheKey, String region, String puuid, int count,
                                                  HttpEntity<Void> entity, PlayerSearchRequest req) {
        List<MatchInfo> list = fetchValorantMatchSummariesFromLifetime(region, puuid, count);
        if (list == null) {
            list = fetchValorantSummariesFromV3Fallback(region, puuid, count, entity, req);
        }
        if (list == null) {
            list = List.of();
        }
        valorantLifetimeSearchCache.put(matchCacheKey,
                new Cached<>(list, System.currentTimeMillis() + VALORANT_SEARCH_API_CACHE_TTL_MS));
        return list;
    }

    /**
     * Riot ID(닉·태그)로 Henrik 계정을 찾은 뒤, 그 응답의 puuid로 MMR을 조회한다.
     * Riot Account API의 puuid(긴 문자열)와 Henrik MMR이 기대하는 puuid가 다를 수 있어 Henrik puuid만 사용한다.
     */
    public ValorantMmrApiResponse fetchMmrForRiotLinkedProfile(String gameName, String tag) {
        if (!valorantApiProperties.hasApiKey()) {
            return null;
        }
        String name = gameName == null ? "" : gameName.trim();
        if (name.isBlank() || tag == null || tag.isBlank()) {
            return null;
        }
        try {
            ValorantPuuidApiResponse acc = getAccountByNameTag(name, tag, false);
            if (acc == null || acc.getData() == null) {
                return null;
            }
            String henrikPuuid = acc.getData().getPuuid();
            if (henrikPuuid == null || henrikPuuid.isBlank()) {
                return null;
            }
            String shard = mapAccountRegionToHenrikShard(acc.getData().getRegion());
            try {
                return getMmr(henrikPuuid.trim(), shard);
            } catch (Exception e) {
                log.debug("Valorant MMR 프로필용 조회 생략: {}", e.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.debug("Valorant 계정/MMR 프로필용 조회 생략: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 1. 계정 정보 조회 (PUUID 확인용)
     * 형식: /valorant/v1/account/{name}/{tag}?api_key={key}
     * <p>DB에 신선한 캐시({@link MatchApiCachePolicy})가 있으면 Henrik 호출을 생략한다.
     */
    public ValorantPuuidApiResponse getAccountByNameTag(String name, String tag) {
        return getAccountByNameTag(name, tag, false);
    }

    public ValorantPuuidApiResponse getAccountByNameTag(String name, String tag, boolean forceRefresh) {
        String n = name == null ? "" : name.trim();
        String t = tag == null ? "" : tag.trim();
        if (t.startsWith("#")) {
            t = t.substring(1).trim();
        }
        if (!forceRefresh) {
            Optional<ValorantPuuidApiResponse> cached = valorantAccountService.findFreshCachedAccountResponse(n, t);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v1/account/{name}/{tag}")
                .buildAndExpand(n, t)
                .encode()
                .toUriString();
        url = appendApiKey(url);

        HttpEntity<Void> entity = valorantHttpEntity();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            ValorantPuuidApiResponse body = objectMapper.readValue(response.getBody(), ValorantPuuidApiResponse.class);
            try {
                valorantAccountService.saveAccount(body);
            } catch (Exception e) {
                log.warn("Valorant 계정 DB 저장 실패 (조회는 반환): {}", e.getMessage());
            }
            return body;
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant 계정 API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 계정 조회 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 2. 간략 전적 리스트 → 3. 매치 상세 조회 후 DB 저장
     * - v1/lifetime/matches 로 match_id 확보
     * - 각 match_id로 v2/match 상세 조회 후 저장
     *
     * @param puuid  플레이어 puuid
     * @param region 리전 (ap, kr 등)
     * @param count  저장할 매치 수 (최대 100)
     * @param mode   게임 모드 필터 (competitive, unrated 등, null이면 전체)
     */
    public void syncValorantRecentMatches(String puuid, String region, int count, String mode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v1/lifetime/matches/{region}/by-puuid/{puuid}")
                .queryParam("size", Math.min(count, 100))
                .buildAndExpand(region, puuid)
                .toUriString();
        if (mode != null && !mode.isBlank()) {
            url = UriComponentsBuilder.fromHttpUrl(url).queryParam("mode", mode).build().toUriString();
        }
        url = appendApiKey(url);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            ValorantLifetimeApiResponse lifetimeResponse = objectMapper.readValue(
                    response.getBody(),
                    ValorantLifetimeApiResponse.class
            );

            if (lifetimeResponse.getData() == null || lifetimeResponse.getData().isEmpty()) {
                return;
            }

            List<String> matchIds = lifetimeResponse.getData().stream()
                    .map(item -> item.getMeta() != null ? item.getMeta().getId() : null)
                    .filter(id -> id != null && !id.isBlank())
                    .limit(count)
                    .collect(Collectors.toList());

            for (String matchId : matchIds) {
                ValorantMatchDetailDto detail = getMatchDetail(matchId);
                if (detail != null) {
                    valorantMatchService.saveMatch(detail);
                    valorantMatchResultService.saveMatchResult(matchId);
                }
            }
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant Lifetime API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 전적 동기화 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 기존 시그니처 호환용 (mode 없이 count만 전달)
     */
    public void syncValorantRecentMatches(String puuid, String region, int count) {
        syncValorantRecentMatches(puuid, region, count, null);
    }

    /**
     * 3. 매치 상세 데이터
     * 형식: /valorant/v2/match/{match_id}?api_key={key}
     *
     * @param matchId 매치 고유 ID (lifetime에서 확보)
     * @return 매치 상세 DTO, 없으면 null
     */
    private static final Object VALORANT_MATCH_DETAIL_THROTTLE = new Object();
    private static volatile long valorantLastMatchDetailCallMs = 0L;
    private static final long VALORANT_MATCH_DETAIL_MIN_INTERVAL_MS = 450L;
    private static final int VALORANT_MATCH_DETAIL_MAX_429_RETRIES = 5;

    /**
     * Henrik 매치 상세(v2/match)는 분당 호출 제한이 빡빡해, 연속 호출 간 최소 간격 + 429 시 재시도한다.
     */
    public ValorantMatchDetailDto getMatchDetail(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return null;
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v2/match/{matchId}")
                .buildAndExpand(matchId)
                .toUriString();
        url = appendApiKey(url);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());
        long backoffBaseMs = 1200L;

        for (int attempt = 1; attempt <= VALORANT_MATCH_DETAIL_MAX_429_RETRIES; attempt++) {
            throttleValorantMatchDetail();
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );
                return valorantMatchJsonService.parseFirstMatch(response.getBody());
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 404) {
                    return null;
                }
                if (e.getStatusCode().value() == 429 && attempt < VALORANT_MATCH_DETAIL_MAX_429_RETRIES) {
                    long waitMs = parseRetryAfterDelayMs(e);
                    if (waitMs < 0) {
                        waitMs = Math.min(backoffBaseMs * attempt, 30_000L);
                    }
                    log.warn("Valorant 매치 상세 429, {}ms 후 재시도 ({}/{})", waitMs, attempt,
                            VALORANT_MATCH_DETAIL_MAX_429_RETRIES);
                    sleepUnchecked(waitMs);
                    continue;
                }
                if (e.getStatusCode().value() == 429) {
                    throw new RuntimeException(
                            "Valorant 매치 상세 API 호출 한도에 걸렸습니다. 잠시 후 다시 시도해 주세요.");
                }
                throw new RuntimeException("Valorant 매치 상세 API 호출 실패: " + e.getStatusCode() + " / "
                        + e.getResponseBodyAsString());
            } catch (Exception e) {
                throw new RuntimeException("Valorant 매치 상세 조회 오류: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Valorant 매치 상세 API 호출 한도에 걸렸습니다. 잠시 후 다시 시도해 주세요.");
    }

    private static void throttleValorantMatchDetail() {
        synchronized (VALORANT_MATCH_DETAIL_THROTTLE) {
            long now = System.currentTimeMillis();
            long waitMs = valorantLastMatchDetailCallMs + VALORANT_MATCH_DETAIL_MIN_INTERVAL_MS - now;
            if (waitMs > 0) {
                sleepUnchecked(waitMs);
            }
            valorantLastMatchDetailCallMs = System.currentTimeMillis();
        }
    }

    private static long parseRetryAfterDelayMs(HttpStatusCodeException e) {
        if (e.getResponseHeaders() == null) {
            return -1;
        }
        String v = e.getResponseHeaders().getFirst("Retry-After");
        if (v == null || v.isBlank()) {
            return -1;
        }
        try {
            long sec = Long.parseLong(v.trim());
            return Math.min(Math.max(sec * 1000L, 500L), 120_000L);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static void sleepUnchecked(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 4. 실시간 MMR 및 티어 정보
     * 형식: /valorant/v2/by-puuid/mmr/{region}/{puuid}?api_key={key}
     *
     * @param puuid  플레이어 puuid
     * @param region 리전 (ap, kr 등)
     * @return MMR/티어 정보
     */
    public ValorantMmrApiResponse getMmr(String puuid, String region) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v2/by-puuid/mmr/{region}/{puuid}")
                .buildAndExpand(region, puuid)
                .toUriString();
        url = appendApiKey(url);

        HttpEntity<Void> entity = valorantHttpEntity();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return objectMapper.readValue(response.getBody(), ValorantMmrApiResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant MMR API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant MMR 조회 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 5. MMR 변동 이력 (최근 점수 그래프)
     * 형식: /valorant/v1/by-puuid/mmr-history/{region}/{puuid}?api_key={key}
     *
     * @param puuid  플레이어 puuid
     * @param region 리전 (ap, kr 등)
     * @return MMR 변동 이력
     */
    public ValorantMmrHistoryApiResponse getMmrHistory(String puuid, String region) {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v1/by-puuid/mmr-history/{region}/{puuid}")
                .buildAndExpand(region, puuid)
                .toUriString();
        url = appendApiKey(url);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return objectMapper.readValue(response.getBody(), ValorantMmrHistoryApiResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Valorant MMR History API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant MMR History 조회 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 전적 검색 (PlayerSearchResponse 반환)
     */
    public PlayerSearchResponse search(PlayerSearchRequest req) {
        String nickname = req.getGameName() + "#" + req.getTagLine();

        try {
            ValorantPuuidApiResponse accountResponse = getAccountByNameTag(
                    req.getGameName(), req.getTagLine(), Boolean.TRUE.equals(req.getForceRefresh()));
            ValorantPuuidApiResponse.AccountData accountData = accountResponse.getData();
            if (accountData == null) throw new RuntimeException("발로란트 계정을 찾을 수 없습니다.");

            String puuid = accountData.getPuuid();
            String region = resolveShardRegion(req, accountData.getRegion());
            log.info("Valorant 검색 - gameName={}, tagLine={}, region(account/fallback)={}, region(mapped)={}",
                    req.getGameName(), req.getTagLine(),
                    accountData.getRegion() != null && !accountData.getRegion().isBlank()
                            ? accountData.getRegion() : req.getRegion(), region);

            HttpEntity<Void> entity = valorantHttpEntity();
            String cardUrl = accountData.getCard() != null ? accountData.getCard().getSmall() : null;

            int count = Math.min(req.getCount() != null ? req.getCount() : 5, 20);
            String mmrCacheKey = region + "|" + puuid;
            String matchCacheKey = mmrCacheKey + "|" + count;

            if (Boolean.TRUE.equals(req.getForceRefresh())) {
                valorantMmrSearchCache.remove(mmrCacheKey);
                valorantLifetimeSearchCache.keySet().removeIf(k -> k.startsWith(mmrCacheKey + "|"));
            }

            if (Boolean.TRUE.equals(req.getDeferValorantMmr())) {
                List<MatchInfo> matches = resolveValorantMatchesForSearch(
                        matchCacheKey, region, puuid, count, entity, req);
                MatchStats stats = buildStats(matches);
                Map<String, Object> raw = new LinkedHashMap<>();
                raw.put("valorantRegion", region);
                PlayerInfo playerInfo = PlayerInfo.builder()
                        .puuid(puuid).gameName(req.getGameName()).tagLine(req.getTagLine())
                        .tier("…")
                        .avatarUrl(cardUrl)
                        .rawData(raw)
                        .build();
                return PlayerSearchResponse.builder()
                        .success(true).game("valorant").nickname(nickname)
                        .valorantMmrPending(true)
                        .playerInfo(playerInfo).matches(matches).stats(stats)
                        .build();
            }

            Cached<ValorantMmrTierSnapshot> mmrCached = valorantMmrSearchCache.get(mmrCacheKey);
            Cached<List<MatchInfo>> matCached = valorantLifetimeSearchCache.get(matchCacheKey);
            boolean mmrFresh = !Boolean.TRUE.equals(req.getForceRefresh()) && mmrCached != null && mmrCached.fresh();
            boolean matFresh = !Boolean.TRUE.equals(req.getForceRefresh()) && matCached != null && matCached.fresh();

            ValorantMmrTierSnapshot mmr;
            List<MatchInfo> matches;

            if (mmrFresh && matFresh) {
                mmr = mmrCached.value();
                matches = List.copyOf(matCached.value());
            } else if (mmrFresh) {
                mmr = mmrCached.value();
                matches = fetchMatchesAndCache(matchCacheKey, region, puuid, count, entity, req);
            } else if (matFresh) {
                matches = List.copyOf(matCached.value());
                mmr = fetchMmrAndCache(mmrCacheKey, region, puuid, entity);
            } else {
                CompletableFuture<ValorantMmrTierSnapshot> mmrFut = CompletableFuture.supplyAsync(
                        () -> fetchMmrAndCache(mmrCacheKey, region, puuid, entity), ForkJoinPool.commonPool());
                CompletableFuture<List<MatchInfo>> matFut = CompletableFuture.supplyAsync(
                        () -> fetchMatchesAndCache(matchCacheKey, region, puuid, count, entity, req),
                        ForkJoinPool.commonPool());
                mmr = mmrFut.join();
                matches = matFut.join();
            }

            if (matches == null) {
                matches = List.of();
            }
            MatchStats stats = buildStats(matches);

            PlayerInfo playerInfo = PlayerInfo.builder()
                    .puuid(puuid).gameName(req.getGameName()).tagLine(req.getTagLine())
                    .tier(mmr.displayTier())
                    .avatarUrl(cardUrl).build();

            return PlayerSearchResponse.builder()
                    .success(true).game("valorant").nickname(nickname)
                    .playerInfo(playerInfo).matches(matches).stats(stats).build();
        } catch (Exception e) {
            log.error("Valorant 전적 검색 오류 - {}", nickname, e);
            return PlayerSearchResponse.error("valorant", nickname, e.getMessage());
        }
    }

    private List<MatchInfo> resolveValorantMatchesForSearch(String matchCacheKey, String region, String puuid, int count,
                                                           HttpEntity<Void> entity, PlayerSearchRequest req) {
        Cached<List<MatchInfo>> matCached = valorantLifetimeSearchCache.get(matchCacheKey);
        if (!Boolean.TRUE.equals(req.getForceRefresh()) && matCached != null && matCached.fresh()) {
            return List.copyOf(matCached.value());
        }
        return fetchMatchesAndCache(matchCacheKey, region, puuid, count, entity, req);
    }

    private record ValorantMmrTierSnapshot(String tier, String tierName) {
        String displayTier() {
            return tierName == null || tierName.isEmpty() ? tier : tierName;
        }
    }

    private ValorantMmrTierSnapshot fetchValorantMmrForSearch(String region, String puuid, HttpEntity<Void> entity) {
        String tier = "UNRANKED";
        String tierName = "";
        try {
            String mmrUrl = UriComponentsBuilder
                    .fromHttpUrl(valorantApiProperties.getBaseUrl())
                    .path("/valorant/v2/by-puuid/mmr/{region}/{puuid}")
                    .buildAndExpand(region, puuid)
                    .toUriString();
            if (valorantApiProperties.hasApiKey()) {
                mmrUrl = appendApiKey(mmrUrl);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> mmrResp = restTemplate.exchange(mmrUrl, HttpMethod.GET, entity, Map.class).getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> mmrData = mmrResp != null ? (Map<String, Object>) mmrResp.get("data") : null;
            if (mmrData != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> current = (Map<String, Object>) mmrData.get("current_data");
                if (current != null) {
                    tier = String.valueOf(current.getOrDefault("currenttier", 0));
                    tierName = (String) current.getOrDefault("currenttierpatched", "UNRANKED");
                }
            }
        } catch (Exception e) {
            log.warn("Valorant MMR 조회 실패 (region={}): {}", region, e.getMessage());
        }
        return new ValorantMmrTierSnapshot(tier, tierName);
    }

    private static final DateTimeFormatter LIFETIME_PLAYED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * @return 성공 시 비-null 리스트(빈 목록 가능). lifetime 호출 실패 시 null → v3 폴백.
     */
    private List<MatchInfo> fetchValorantMatchSummariesFromLifetime(String region, String puuid, int count) {
        int size = Math.min(Math.max(count, 1), 100);
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v1/lifetime/matches/{region}/by-puuid/{puuid}")
                .queryParam("size", size)
                .buildAndExpand(region, puuid)
                .toUriString();
        url = appendApiKey(url);
        HttpEntity<Void> httpEntity = valorantHttpEntity();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                return List.of();
            }
            ValorantLifetimeApiResponse parsed = objectMapper.readValue(response.getBody(), ValorantLifetimeApiResponse.class);
            if (parsed.getData() == null) {
                return List.of();
            }
            return parsed.getData().stream()
                    .map(this::matchInfoFromLifetimeItem)
                    .filter(Objects::nonNull)
                    .limit(count)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Valorant lifetime 매치 목록 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Henrik lifetime 응답에서 {@code meta.mode}가 비는 경우가 있음(예: 난투). {@code game_mode} 또는 맵 이름으로 보강.
     */
    private static String resolveLifetimeModeDisplay(ValorantLifetimeDataItem.ValorantLifetimeMeta meta) {
        if (meta == null) {
            return "";
        }
        String m = meta.getMode() != null ? meta.getMode().trim() : "";
        if (!m.isEmpty()) {
            return m;
        }
        String gm = meta.getGameMode() != null ? meta.getGameMode().trim() : "";
        if (!gm.isEmpty()) {
            return gm;
        }
        ValorantLifetimeDataItem.MapRef map = meta.getMap();
        if (map != null && map.getName() != null && !map.getName().isBlank()) {
            String lower = map.getName().trim().toLowerCase(Locale.ROOT);
            if (lower.startsWith("skirmish")) {
                return "Skirmish";
            }
            if (lower.contains("deathmatch")) {
                return "Deathmatch";
            }
        }
        return "";
    }

    private MatchInfo matchInfoFromLifetimeItem(ValorantLifetimeDataItem item) {
        if (item.getMeta() == null || item.getStats() == null) {
            return null;
        }
        ValorantLifetimeDataItem.ValorantLifetimeMeta meta = item.getMeta();
        ValorantLifetimeDataItem.ValorantLifetimeStats s = item.getStats();
        String mid = meta.getId();
        if (mid == null || mid.isBlank()) {
            return null;
        }
        ValorantLifetimeDataItem.CharacterRef c = s.getCharacter();
        String agent = c != null && c.getName() != null ? c.getName() : "";
        int kills = s.getKills();
        int deaths = s.getDeaths();
        int assists = s.getAssists();
        double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;
        boolean win = inferValorantLifetimeWin(s.getTeam(), item.getTeams());
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("score", s.getScore());
        ValorantLifetimeDataItem.Shots shots = s.getShots();
        extras.put("headshots", shots != null ? shots.getHead() : 0);
        extras.put("bodyshots", shots != null ? shots.getBody() : 0);
        if (meta.getMap() != null && meta.getMap().getName() != null) {
            extras.put("map", meta.getMap().getName());
        }
        ValorantLifetimeDataItem.ValorantLifetimeTeams teams = item.getTeams();
        if (teams != null) {
            appendValorantRoundStats(extras, s.getScore(), teams.getRed(), teams.getBlue(), s.getTeam());
        }
        return MatchInfo.builder()
                .matchId(mid)
                .gameMode(resolveLifetimeModeDisplay(meta))
                .agent(agent)
                .win(win)
                .kills(kills).deaths(deaths).assists(assists)
                .kda(Math.round(kda * 100.0) / 100.0)
                .playedAt(formatLifetimeStartedAt(meta.getStartedAt()))
                .extras(extras)
                .build();
    }

    /**
     * 라운드 수·전투 점수로 extras를 채운다. {@code valorantRoundScore}는 플레이어 소속 팀(아군) 라운드를 왼쪽에 둔다.
     */
    private static void appendValorantRoundStats(Map<String, Object> extras, int combatScore,
                                                 int redRounds, int blueRounds, String playerTeam) {
        int played = redRounds + blueRounds;
        if (played <= 0) {
            return;
        }
        extras.put("roundsRed", redRounds);
        extras.put("roundsBlue", blueRounds);
        String t = playerTeam != null ? playerTeam.trim().toLowerCase(Locale.ROOT) : "";
        int allyRounds;
        int enemyRounds;
        if ("red".equals(t)) {
            allyRounds = redRounds;
            enemyRounds = blueRounds;
        } else if ("blue".equals(t)) {
            allyRounds = blueRounds;
            enemyRounds = redRounds;
        } else {
            allyRounds = redRounds;
            enemyRounds = blueRounds;
        }
        extras.put("valorantRoundScore", allyRounds + " : " + enemyRounds);
        double avg = (double) combatScore / played;
        extras.put("avgCombatScorePerRound", Math.round(avg * 10.0) / 10.0);
    }

    private static boolean inferValorantLifetimeWin(String team, ValorantLifetimeDataItem.ValorantLifetimeTeams teams) {
        if (team == null || teams == null) {
            return false;
        }
        int r = teams.getRed();
        int b = teams.getBlue();
        String t = team.trim().toLowerCase(java.util.Locale.ROOT);
        if ("red".equals(t)) {
            return r > b;
        }
        if ("blue".equals(t)) {
            return b > r;
        }
        return false;
    }

    private String formatLifetimeStartedAt(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.of("Asia/Seoul")).format(LIFETIME_PLAYED_AT);
        } catch (Exception e) {
            return iso;
        }
    }

    @SuppressWarnings("unchecked")
    private List<MatchInfo> fetchValorantSummariesFromV3Fallback(String region, String puuid, int count,
                                                                   HttpEntity<Void> entity, PlayerSearchRequest req) {
        String matchUrl = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v3/by-puuid/matches/{region}/{puuid}")
                .queryParam("size", count)
                .buildAndExpand(region, puuid)
                .toUriString();
        if (valorantApiProperties.hasApiKey()) {
            matchUrl = appendApiKey(matchUrl);
        }
        Map<String, Object> matchResp = restTemplate.exchange(matchUrl, HttpMethod.GET, entity, Map.class).getBody();
        List<Map<String, Object>> matchData = matchResp != null ? (List<Map<String, Object>>) matchResp.get("data") : List.of();
        List<MatchInfo> matches = new ArrayList<>();
        for (Map<String, Object> matchMap : matchData) {
            try {
                String mid = valorantMatchIdFromListMap(matchMap);
                if (mid == null || mid.isBlank()) {
                    continue;
                }
                MatchInfo one;
                Optional<ValorantMatch> cached = valorantMatchDetailRepository.findByMatchIdWithPlayers(mid);
                if (cached.isPresent() && !Boolean.TRUE.equals(req.getForceRefresh())
                        && !MatchApiCachePolicy.isStale(cached.get().getApiCachedAt())) {
                    one = matchInfoFromValorantEntity(cached.get(), puuid);
                } else {
                    one = parseOneValorantMatchFromMap(matchMap, puuid);
                }
                if (one != null) {
                    matches.add(one);
                }
            } catch (Exception e) {
                log.warn("Valorant v3 폴백 매치 처리 실패", e);
            }
        }
        return matches;
    }

    @SuppressWarnings("unchecked")
    private String valorantMatchIdFromListMap(Map<String, Object> match) {
        if (match == null) return null;
        Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");
        if (metadata == null) return null;
        Object id = metadata.get("matchid");
        if (id == null) id = metadata.get("matchId");
        return id != null ? String.valueOf(id) : null;
    }

    private MatchInfo matchInfoFromValorantEntity(ValorantMatch m, String puuid) {
        if (m.getPlayers() == null) return null;
        ValorantMatchPlayer me = m.getPlayers().stream()
                .filter(p -> puuid.equals(p.getPuuid())).findFirst().orElse(null);
        if (me == null) return null;
        int kills = me.getKills() != null ? me.getKills() : 0;
        int deaths = me.getDeaths() != null ? me.getDeaths() : 0;
        int assists = me.getAssists() != null ? me.getAssists() : 0;
        double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;
        Map<String, Object> extras = new LinkedHashMap<>();
        int combatScore = me.getScore() != null ? me.getScore() : 0;
        extras.put("score", combatScore);
        extras.put("headshots", me.getHeadshots());
        extras.put("bodyshots", me.getBodyshots());
        if (m.getRedRoundsWon() != null && m.getBlueRoundsWon() != null) {
            appendValorantRoundStats(extras, combatScore, m.getRedRoundsWon(), m.getBlueRoundsWon(), me.getTeam());
        }
        return MatchInfo.builder()
                .matchId(m.getMatchId())
                .gameMode(m.getMode() != null ? m.getMode() : "")
                .agent(me.getAgent() != null ? me.getAgent() : "")
                .win(me.isWin())
                .kills(kills).deaths(deaths).assists(assists)
                .kda(Math.round(kda * 100.0) / 100.0)
                .playedAt(m.getGameStartPatched() != null ? m.getGameStartPatched() : "")
                .extras(extras).build();
    }

    @SuppressWarnings("unchecked")
    private MatchInfo parseOneValorantMatchFromMap(Map<String, Object> match, String puuid) {
        try {
            Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");
            Map<String, Object> players = (Map<String, Object>) match.get("players");
            Map<String, Object> teams = (Map<String, Object>) match.get("teams");
            if (metadata == null || players == null) return null;
            List<Map<String, Object>> allPlayers = (List<Map<String, Object>>) players.get("all_players");
            if (allPlayers == null) return null;
            Map<String, Object> me = allPlayers.stream()
                    .filter(p -> puuid.equals(p.get("puuid"))).findFirst().orElse(null);
            if (me == null) return null;
            Map<String, Object> stats = (Map<String, Object>) me.get("stats");
            int kills = stats != null ? toInt(stats.get("kills")) : 0;
            int deaths = stats != null ? toInt(stats.get("deaths")) : 0;
            int assists = stats != null ? toInt(stats.get("assists")) : 0;
            double kda = deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;
            String myTeam = (String) me.get("team");
            boolean win = false;
            if (teams != null && myTeam != null) {
                Map<String, Object> myTeamData = (Map<String, Object>) teams.get(myTeam.toLowerCase());
                if (myTeamData != null) win = Boolean.TRUE.equals(myTeamData.get("has_won"));
            }
            Map<String, Object> extras = new LinkedHashMap<>();
            int scoreVal = 0;
            if (stats != null) {
                extras.put("score", stats.get("score"));
                extras.put("headshots", stats.get("headshots"));
                extras.put("bodyshots", stats.get("bodyshots"));
                scoreVal = toInt(stats.get("score"));
            }
            Integer rr = null;
            Integer bb = null;
            if (teams != null) {
                Map<String, Object> redT = (Map<String, Object>) teams.get("red");
                Map<String, Object> blueT = (Map<String, Object>) teams.get("blue");
                if (redT != null) {
                    rr = toInt(redT.get("rounds_won"));
                }
                if (blueT != null) {
                    bb = toInt(blueT.get("rounds_won"));
                }
            }
            if (rr != null && bb != null) {
                appendValorantRoundStats(extras, scoreVal, rr, bb, myTeam);
            }
            return MatchInfo.builder()
                    .matchId(valorantMatchIdFromListMap(match))
                    .gameMode((String) metadata.getOrDefault("mode", ""))
                    .agent((String) me.getOrDefault("character", ""))
                    .win(win).kills(kills).deaths(deaths).assists(assists)
                    .kda(Math.round(kda * 100.0) / 100.0)
                    .playedAt((String) metadata.getOrDefault("game_start_patched", ""))
                    .extras(extras).build();
        } catch (Exception e) {
            log.warn("Valorant 매치 파싱 실패", e);
            return null;
        }
    }

    private MatchStats buildStats(List<MatchInfo> matches) {
        if (matches.isEmpty()) return MatchStats.builder().build();
        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getWin())).count();
        double avgK = matches.stream().mapToInt(m -> m.getKills() != null ? m.getKills() : 0).average().orElse(0);
        double avgD = matches.stream().mapToInt(m -> m.getDeaths() != null ? m.getDeaths() : 0).average().orElse(0);
        double avgA = matches.stream().mapToInt(m -> m.getAssists() != null ? m.getAssists() : 0).average().orElse(0);
        double avgKda = matches.stream().mapToDouble(m -> m.getKda() != null ? m.getKda() : 0).average().orElse(0);
        String mostAgent = matches.stream().filter(m -> m.getAgent() != null)
                .collect(Collectors.groupingBy(MatchInfo::getAgent, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("-");
        return MatchStats.builder()
                .totalGames(matches.size()).wins(wins).losses(matches.size() - wins)
                .winRate(Math.round((double) wins / matches.size() * 1000.0) / 10.0)
                .avgKills(Math.round(avgK * 10.0) / 10.0).avgDeaths(Math.round(avgD * 10.0) / 10.0)
                .avgAssists(Math.round(avgA * 10.0) / 10.0).avgKda(Math.round(avgKda * 100.0) / 100.0)
                .mostUsedChampionOrAgent(mostAgent).build();
    }

    private int toInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 0;
    }
}
