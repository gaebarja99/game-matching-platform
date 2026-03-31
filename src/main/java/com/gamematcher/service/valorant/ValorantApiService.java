package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.config.api.ValorantApiProperties;
import com.gamematcher.dto.valorant.ValorantLifetimeApiResponse;
import com.gamematcher.dto.valorant.ValorantLifetimeDataItem;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.dto.valorant.ValorantMmrHistoryApiResponse;
import com.gamematcher.dto.valorant.ValorantPlayerDto;
import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import com.gamematcher.service.MatchApiCachePolicy;
import com.gamematcher.service.riot.RiotApiService;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.dto.search.PlayerSearchResponse.*;
import com.gamematcher.dto.search.ValorantSearchMmrRequest;
import com.gamematcher.dto.search.ValorantSearchMmrResponse;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;
import com.gamematcher.repository.match.ValorantMatchDetailRepository;
import com.gamematcher.util.RateLimitMessageUtil;
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
import java.util.concurrent.ConcurrentHashMap;
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
    private final RiotApiService riotApiService;

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
     * 병렬 API 호출은 레이트리밋에 걸리기 쉬워 순차 호출 + 캐시 조합을 쓴다.
     */
    private static final long VALORANT_SEARCH_API_CACHE_TTL_MS = Duration.ofMinutes(3).toMillis();

    private final ConcurrentHashMap<String, Cached<ValorantMmrTierSnapshot>> valorantMmrSearchCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cached<List<MatchInfo>>> valorantLifetimeSearchCache = new ConcurrentHashMap<>();

    private record Cached<T>(T value, long expiresAtMs) {
        boolean fresh() {
            return System.currentTimeMillis() < expiresAtMs;
        }
    }

    private ValorantPuuidApiResponse getAccountByNameTagV2(String name, String tag) throws Exception {
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v2/account/{name}/{tag}")
                .queryParam("force", true)
                .buildAndExpand(name, tag)
                .encode()
                .toUriString();
        url = appendApiKey(url);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                valorantHttpEntity(),
                String.class
        );
        return readAccountResponse(response.getBody());
    }

    private ValorantPuuidApiResponse getAccountByOfficialRiotId(String name, String tag, String fallbackRegion) {
        var riotAccount = riotApiService.getAccountByRiotId(name, tag);
        if (riotAccount == null || riotAccount.getPuuid() == null || riotAccount.getPuuid().isBlank()) {
            throw new RuntimeException("Riot Account API returned no puuid.");
        }

        ValorantPuuidApiResponse.AccountData data = new ValorantPuuidApiResponse.AccountData();
        data.setPuuid(riotAccount.getPuuid());
        data.setName(riotAccount.getGameName());
        data.setTag(riotAccount.getTagLine());
        data.setRegion(fallbackRegion);

        ValorantPuuidApiResponse response = new ValorantPuuidApiResponse();
        response.setStatus(200);
        response.setData(data);
        return response;
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

    private List<String> resolveCandidateRegions(PlayerSearchRequest req, String accountApiRegion) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(resolveShardRegion(req, accountApiRegion));
        candidates.add("kr");
        candidates.add("ap");
        candidates.add("na");
        candidates.add("eu");
        candidates.add("latam");
        candidates.add("br");
        return new ArrayList<>(candidates);
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
        if (!forceRefresh) {
            Optional<ValorantPuuidApiResponse> cached = valorantAccountService.findFreshCachedAccountResponse(name, tag);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        HttpEntity<Void> entity = valorantHttpEntity();
        List<String> candidateUrls = List.of(
                UriComponentsBuilder
                        .fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v2/account/{name}/{tag}")
                        .queryParam("force", true)
                        .buildAndExpand(name, tag)
                        .encode()
                        .toUriString(),
                UriComponentsBuilder
                        .fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v1/account/{name}/{tag}")
                        .queryParam("force", true)
                        .buildAndExpand(name, tag)
                        .encode()
                        .toUriString()
        );

        RuntimeException lastError = null;
        for (String candidateUrl : candidateUrls) {
            String url = appendApiKey(candidateUrl);
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );
                ValorantPuuidApiResponse body = readAccountResponse(response.getBody());
                try {
                    valorantAccountService.saveAccount(body);
                } catch (Exception e) {
                    log.warn("Valorant 계정 DB 저장 실패 (조회는 반환): {}", e.getMessage());
                }
                return body;
            } catch (HttpStatusCodeException e) {
                lastError = e.getStatusCode().value() == 404
                        ? new RuntimeException("입력한 Riot ID로 발로란트 계정을 찾을 수 없습니다.", e)
                        : new RuntimeException("발로란트 계정 조회에 실패했습니다: " + e.getStatusCode(), e);
            } catch (Exception e) {
                lastError = new RuntimeException("발로란트 계정 정보를 불러오지 못했습니다: " + e.getMessage(), e);
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new RuntimeException("발로란트 계정 정보를 불러오지 못했습니다.");
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
    public ValorantPuuidApiResponse getAccountByPuuid(String puuid) {
        List<String> candidateUrls = List.of(
                UriComponentsBuilder.fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v2/account/{puuid}")
                        .queryParam("force", true)
                        .buildAndExpand(puuid)
                        .toUriString(),
                UriComponentsBuilder.fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v1/account/{puuid}")
                        .queryParam("force", true)
                        .buildAndExpand(puuid)
                        .toUriString(),
                UriComponentsBuilder.fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v2/by-puuid/account/{puuid}")
                        .queryParam("force", true)
                        .buildAndExpand(puuid)
                        .toUriString(),
                UriComponentsBuilder.fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v1/by-puuid/account/{puuid}")
                        .queryParam("force", true)
                        .buildAndExpand(puuid)
                        .toUriString()
        );

        HttpEntity<Void> entity = valorantHttpEntity();
        RuntimeException lastError = null;

        for (String candidateUrl : candidateUrls) {
            String url = appendApiKey(candidateUrl);
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                return readAccountResponse(response.getBody());
            } catch (HttpStatusCodeException e) {
                lastError = new RuntimeException(
                        "Valorant account-by-puuid API call failed: " + e.getStatusCode() + " / " + e.getResponseBodyAsString(),
                        e
                );
            } catch (Exception e) {
                lastError = new RuntimeException("Valorant account-by-puuid lookup failed: " + e.getMessage(), e);
            }
        }

        throw lastError != null ? lastError : new RuntimeException("Valorant account-by-puuid lookup failed.");
    }

    public ValorantMatchApiResponse getRecentMatchesByPuuid(String region, String puuid, int size) {
        List<String> candidateUrls = List.of(
                UriComponentsBuilder.fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v3/by-puuid/matches/{region}/{puuid}")
                        .queryParam("size", Math.min(Math.max(size, 1), 10))
                        .buildAndExpand(region, puuid)
                        .toUriString(),
                UriComponentsBuilder.fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v2/by-puuid/matches/{region}/{puuid}")
                        .queryParam("size", Math.min(Math.max(size, 1), 10))
                        .buildAndExpand(region, puuid)
                        .toUriString()
        );

        HttpEntity<Void> entity = valorantHttpEntity();
        RuntimeException lastError = null;

        for (String candidateUrl : candidateUrls) {
            String url = appendApiKey(candidateUrl);
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                return objectMapper.readValue(response.getBody(), ValorantMatchApiResponse.class);
            } catch (HttpStatusCodeException e) {
                lastError = new RuntimeException(
                        "Valorant recent matches API call failed: " + e.getStatusCode() + " / " + e.getResponseBodyAsString(),
                        e
                );
            } catch (Exception e) {
                lastError = new RuntimeException("Valorant recent matches lookup failed: " + e.getMessage(), e);
            }
        }

        throw lastError != null ? lastError : new RuntimeException("Valorant recent matches lookup failed.");
    }

    public PlayerCardSnapshot getLatestPlayerCardFromRecentMatches(String region, String puuid) {
        ValorantMatchApiResponse response = getRecentMatchesByPuuid(region, puuid, 5);
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new RuntimeException("No recent matches returned.");
        }

        for (ValorantMatchDetailDto match : response.getData()) {
            if (match == null || match.getPlayers() == null || match.getPlayers().getAllPlayers() == null) {
                continue;
            }

            ValorantPlayerDto player = match.getPlayers().getAllPlayers().stream()
                    .filter(candidate -> puuid.equals(candidate.getPuuid()))
                    .findFirst()
                    .orElse(null);
            if (player == null) {
                continue;
            }

            String cardId = player.getPlayerCard();
            String cardImageUrl = player.getAssets() != null && player.getAssets().getCard() != null
                    ? player.getAssets().getCard().getSmall()
                    : null;

            if (cardId == null || cardId.isBlank()) {
                throw new RuntimeException("Recent matches found but player_card is empty.");
            }

            return new PlayerCardSnapshot(
                    match.getMetadata() != null ? match.getMetadata().getMatchId() : null,
                    cardId,
                    cardImageUrl
            );
        }

        throw new RuntimeException("Recent matches found but matching player was not present.");
    }

    private ValorantPuuidApiResponse readAccountResponse(String rawBody) throws Exception {
        JsonNode root = objectMapper.readTree(rawBody);
        return parseAccountResponse(root);
    }

    private ValorantPuuidApiResponse parseAccountResponse(JsonNode root) {
        ValorantPuuidApiResponse response = new ValorantPuuidApiResponse();
        response.setStatus(root.path("status").asInt(200));

        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            return response;
        }

        ValorantPuuidApiResponse.AccountData accountData = new ValorantPuuidApiResponse.AccountData();
        accountData.setPuuid(textOrNull(dataNode, "puuid"));
        accountData.setRegion(textOrNull(dataNode, "region"));
        accountData.setAccountLevel(dataNode.path("account_level").isNumber() ? dataNode.path("account_level").asInt() : null);
        accountData.setName(textOrNull(dataNode, "name"));
        accountData.setTag(textOrNull(dataNode, "tag"));
        accountData.setLastUpdate(firstText(dataNode, "last_update", "updated_at"));
        accountData.setLastUpdateRaw(dataNode.path("last_update_raw").isNumber() ? dataNode.path("last_update_raw").asLong() : null);

        JsonNode cardNode = dataNode.has("card") ? dataNode.path("card") : dataNode.path("player_card");
        if (!cardNode.isMissingNode() && !cardNode.isNull()) {
            ValorantPuuidApiResponse.Card card = new ValorantPuuidApiResponse.Card();
            if (cardNode.isTextual()) {
                card.setId(cardNode.asText());
            } else {
                card.setId(textOrNull(cardNode, "id"));
                card.setSmall(textOrNull(cardNode, "small"));
                card.setLarge(textOrNull(cardNode, "large"));
                card.setWide(textOrNull(cardNode, "wide"));
            }
            accountData.setCard(card);
        }

        response.setData(accountData);
        return response;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = textOrNull(node, fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }


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
            throw new RuntimeException("Valorant 매치 상세 API 호출 실패: " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Valorant 매치 상세 조회 오류: " + e.getMessage(), e);
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

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

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

            ValorantMmrTierSnapshot mmr = Optional.ofNullable(valorantMmrSearchCache.get(mmrCacheKey))
                    .filter(Cached::fresh)
                    .map(Cached::value)
                    .orElseGet(() -> {
                        ValorantMmrTierSnapshot m = fetchValorantMmrForSearch(region, puuid, entity);
                        valorantMmrSearchCache.put(mmrCacheKey,
                                new Cached<>(m, System.currentTimeMillis() + VALORANT_SEARCH_API_CACHE_TTL_MS));
                        return m;
                    });
            List<MatchInfo> matches = Optional.ofNullable(valorantLifetimeSearchCache.get(matchCacheKey))
                    .filter(Cached::fresh)
                    .map(c -> List.copyOf(c.value()))
                    .orElse(null);
            if (matches == null) {
                matches = fetchValorantMatchSummariesFromLifetime(region, puuid, count);
                if (matches == null) {
                    matches = fetchValorantSummariesFromV3Fallback(region, puuid, count, entity, req);
                }
                if (matches != null) {
                    valorantLifetimeSearchCache.put(matchCacheKey,
                            new Cached<>(matches, System.currentTimeMillis() + VALORANT_SEARCH_API_CACHE_TTL_MS));
                }
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
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("429")) {
                return PlayerSearchResponse.error("valorant", nickname, RateLimitMessageUtil.RATE_LIMIT_USER_MESSAGE);
            }
            return PlayerSearchResponse.error(
                    "valorant",
                    nickname,
                    errorMessage != null ? errorMessage : "발로란트 전적을 불러오지 못했습니다."
            );
        }
    }

    /**
     * Records: 1차 검색에서 {@code deferValorantMmr} 로 티어를 미룬 뒤, Henrik MMR만 조회한다.
     */
    public ValorantSearchMmrResponse resolveMmrForSearch(ValorantSearchMmrRequest request) {
        if (request.getPuuid() == null || request.getPuuid().isBlank()) {
            return ValorantSearchMmrResponse.fail("puuid가 필요합니다.");
        }
        String puuid = request.getPuuid();
        String regionInput = request.getRegion();
        final String region = (regionInput == null || regionInput.isBlank()) ? "kr" : regionInput;
        String mmrCacheKey = region + "|" + puuid;
        HttpEntity<Void> entity = valorantHttpEntity();
        if (Boolean.TRUE.equals(request.getForceRefresh())) {
            valorantMmrSearchCache.remove(mmrCacheKey);
        }
        ValorantMmrTierSnapshot mmr = Optional.ofNullable(valorantMmrSearchCache.get(mmrCacheKey))
                .filter(Cached::fresh)
                .map(Cached::value)
                .orElseGet(() -> {
                    ValorantMmrTierSnapshot m = fetchValorantMmrForSearch(region, puuid, entity);
                    valorantMmrSearchCache.put(mmrCacheKey,
                            new Cached<>(m, System.currentTimeMillis() + VALORANT_SEARCH_API_CACHE_TTL_MS));
                    return m;
                });
        return ValorantSearchMmrResponse.ok(mmr.displayTier());
    }

    private ValorantPuuidApiResponse.AccountData loadAccountForSearch(PlayerSearchRequest req) {
        try {
            ValorantPuuidApiResponse accountResponse = getAccountByNameTag(
                    req.getGameName(),
                    req.getTagLine(),
                    Boolean.TRUE.equals(req.getForceRefresh())
            );
            return accountResponse.getData();
        } catch (RuntimeException firstError) {
            if (!isValorantAccountNotFound(firstError)) {
                throw firstError;
            }

            try {
                ValorantPuuidApiResponse fallbackResponse = getAccountByNameTagV2(req.getGameName(), req.getTagLine());
                if (fallbackResponse != null && fallbackResponse.getData() != null
                        && fallbackResponse.getData().getPuuid() != null
                        && !fallbackResponse.getData().getPuuid().isBlank()) {
                    try {
                        valorantAccountService.saveAccount(fallbackResponse);
                    } catch (Exception saveError) {
                        log.warn("Valorant v2 fallback account cache save failed: {}", saveError.getMessage());
                    }
                    log.info("Recovered Valorant account lookup through v2 fallback for {}#{}",
                            req.getGameName(), req.getTagLine());
                    return fallbackResponse.getData();
                }
            } catch (HttpStatusCodeException fallbackError) {
                log.warn("Valorant v2 fallback failed for {}#{} with status {}",
                        req.getGameName(), req.getTagLine(), fallbackError.getStatusCode());
            } catch (Exception fallbackError) {
                log.warn("Valorant v2 fallback exception for {}#{}: {}",
                        req.getGameName(), req.getTagLine(), fallbackError.getMessage());
            }

            try {
                ValorantPuuidApiResponse riotFallback = getAccountByOfficialRiotId(
                        req.getGameName(),
                        req.getTagLine(),
                        req.getRegion() != null && !req.getRegion().isBlank() ? req.getRegion() : "kr"
                );
                if (riotFallback.getData() != null && riotFallback.getData().getPuuid() != null
                        && !riotFallback.getData().getPuuid().isBlank()) {
                    try {
                        valorantAccountService.saveAccount(riotFallback);
                    } catch (Exception saveError) {
                        log.warn("Valorant Riot fallback account cache save failed: {}", saveError.getMessage());
                    }
                    log.info("Recovered Valorant account lookup through Riot account-v1 fallback for {}#{}",
                            req.getGameName(), req.getTagLine());
                    return riotFallback.getData();
                }
            } catch (Exception riotFallbackError) {
                log.warn("Valorant Riot account-v1 fallback failed for {}#{}: {}",
                        req.getGameName(), req.getTagLine(), riotFallbackError.getMessage());
            }

            Optional<ValorantPuuidApiResponse> cachedAccount =
                    valorantAccountService.findAnyCachedAccountResponse(req.getGameName(), req.getTagLine());
            if (cachedAccount.isPresent() && cachedAccount.get().getData() != null) {
                log.info("Using stale cached Valorant account for {}#{}", req.getGameName(), req.getTagLine());
                return cachedAccount.get().getData();
            }
            throw firstError;
        }
    }

    private boolean isValorantAccountNotFound(RuntimeException error) {
        String message = error.getMessage();
        return message != null
                && (message.contains("Account not found")
                || message.contains("발로란트 계정을 찾을 수 없습니다.")
                || message.contains("입력한 Riot ID로 발로란트 계정을 찾을 수 없습니다."));
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

    private ValorantMmrTierSnapshot fetchValorantMmrByRiotIdForSearch(String region, String gameName, String tagLine, HttpEntity<Void> entity) {
        String tier = "UNRANKED";
        String tierName = "";
        try {
            String mmrUrl = UriComponentsBuilder
                    .fromHttpUrl(valorantApiProperties.getBaseUrl())
                    .path("/valorant/v3/mmr/{region}/{platform}/{name}/{tag}")
                    .buildAndExpand(region, "pc", gameName, tagLine)
                    .encode()
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
                Map<String, Object> current = (Map<String, Object>) mmrData.get("current");
                if (current != null) {
                    Object tierObj = current.get("tier");
                    if (tierObj instanceof Map<?, ?> tierMap) {
                        Object id = tierMap.get("id");
                        Object name = tierMap.get("name");
                        tier = id != null ? String.valueOf(id) : tier;
                        tierName = name != null ? String.valueOf(name) : tierName;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Valorant Riot ID MMR lookup failed (region={}, name={}, tag={}): {}",
                    region, gameName, tagLine, e.getMessage());
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

    @SuppressWarnings("unchecked")
    private List<MatchInfo> fetchValorantSummariesByRiotIdFallback(String region, String gameName, String tagLine,
                                                                   int count, HttpEntity<Void> entity) {
        int size = Math.min(Math.max(count, 1), 10);
        List<String> candidateUrls = List.of(
                UriComponentsBuilder
                        .fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v3/matches/{region}/{name}/{tag}")
                        .queryParam("size", size)
                        .buildAndExpand(region, gameName, tagLine)
                        .encode()
                        .toUriString(),
                UriComponentsBuilder
                        .fromHttpUrl(valorantApiProperties.getBaseUrl())
                        .path("/valorant/v4/matches/{region}/{platform}/{name}/{tag}")
                        .queryParam("size", size)
                        .buildAndExpand(region, "pc", gameName, tagLine)
                        .encode()
                        .toUriString()
        );

        for (String candidateUrl : candidateUrls) {
            String matchUrl = appendApiKey(candidateUrl);
            try {
                Map<String, Object> matchResp = restTemplate.exchange(matchUrl, HttpMethod.GET, entity, Map.class).getBody();
                List<Map<String, Object>> matchData = matchResp != null
                        ? (List<Map<String, Object>>) matchResp.getOrDefault("data", List.of())
                        : List.of();
                List<MatchInfo> matches = new ArrayList<>();
                for (Map<String, Object> matchMap : matchData) {
                    MatchInfo one = parseOneValorantMatchFromMap(matchMap, null, gameName, tagLine);
                    if (one != null) {
                        matches.add(one);
                    }
                }
                if (!matches.isEmpty()) {
                    log.info("Recovered Valorant matches through Riot ID fallback for {}#{} in region {} via {}",
                            gameName, tagLine, region, candidateUrl);
                    return matches;
                }
            } catch (HttpStatusCodeException e) {
                log.warn("Valorant Riot ID match fallback failed for {}#{} in region {} via {} with status {}",
                        gameName, tagLine, region, candidateUrl, e.getStatusCode());
            } catch (Exception e) {
                log.warn("Valorant Riot ID match fallback exception for {}#{} in region {} via {}: {}",
                        gameName, tagLine, region, candidateUrl, e.getMessage());
            }
        }

        return List.of();
    }

    private List<MatchInfo> fetchValorantStoredMatchesByRiotIdFallback(String region, String gameName, String tagLine, int count) {
        int size = Math.min(Math.max(count, 1), 100);
        String url = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v1/stored-matches/{region}/{name}/{tag}")
                .queryParam("size", size)
                .buildAndExpand(region, gameName, tagLine)
                .encode()
                .toUriString();
        url = appendApiKey(url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, valorantHttpEntity(), String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                return List.of();
            }
            ValorantLifetimeApiResponse parsed = objectMapper.readValue(response.getBody(), ValorantLifetimeApiResponse.class);
            if (parsed.getData() == null || parsed.getData().isEmpty()) {
                return List.of();
            }
            log.info("Recovered Valorant matches through stored-matches fallback for {}#{} in region {}",
                    gameName, tagLine, region);
            return parsed.getData().stream()
                    .map(this::matchInfoFromLifetimeItem)
                    .filter(Objects::nonNull)
                    .limit(count)
                    .collect(Collectors.toList());
        } catch (HttpStatusCodeException e) {
            log.warn("Valorant stored-matches fallback failed for {}#{} in region {} with status {}",
                    gameName, tagLine, region, e.getStatusCode());
            return List.of();
        } catch (Exception e) {
            log.warn("Valorant stored-matches fallback exception for {}#{} in region {}: {}",
                    gameName, tagLine, region, e.getMessage());
            return List.of();
        }
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
                .gameMode(meta.getMode() != null ? meta.getMode() : "")
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
        int size = Math.min(Math.max(count, 1), 10);
        String matchUrl = UriComponentsBuilder
                .fromHttpUrl(valorantApiProperties.getBaseUrl())
                .path("/valorant/v3/by-puuid/matches/{region}/{puuid}")
                .queryParam("size", size)
                .buildAndExpand(region, puuid)
                .toUriString();
        if (valorantApiProperties.hasApiKey()) {
            matchUrl = appendApiKey(matchUrl);
        }
        Map<String, Object> matchResp;
        try {
            matchResp = restTemplate.exchange(matchUrl, HttpMethod.GET, entity, Map.class).getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("Valorant v3 매치 fallback 실패: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.warn("Valorant v3 매치 fallback 오류: {}", e.getMessage());
            return List.of();
        }
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
                    one = parseOneValorantMatchFromMap(matchMap, puuid, req.getGameName(), req.getTagLine());
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
    private MatchInfo parseOneValorantMatchFromMap(Map<String, Object> match, String puuid, String gameName, String tagLine) {
        try {
            Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");
            Map<String, Object> players = (Map<String, Object>) match.get("players");
            Map<String, Object> teams = (Map<String, Object>) match.get("teams");
            if (metadata == null || players == null) return null;
            List<Map<String, Object>> allPlayers = (List<Map<String, Object>>) players.get("all_players");
            if (allPlayers == null) return null;
            Map<String, Object> me = allPlayers.stream()
                    .filter(p -> matchesValorantPlayerIdentity(p, puuid, gameName, tagLine))
                    .findFirst()
                    .orElse(null);
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

    private boolean matchesValorantPlayerIdentity(Map<String, Object> player, String puuid, String gameName, String tagLine) {
        if (player == null) {
            return false;
        }
        if (puuid != null && !puuid.isBlank() && puuid.equals(player.get("puuid"))) {
            return true;
        }
        String playerName = Objects.toString(player.get("name"), "").trim();
        String playerTag = Objects.toString(player.get("tag"), "").trim();
        return gameName != null && tagLine != null
                && gameName.trim().equalsIgnoreCase(playerName)
                && tagLine.trim().equalsIgnoreCase(playerTag);
    }

    private boolean looksLikeOfficialRiotPuuid(String puuid) {
        return puuid != null && puuid.length() > 50;
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

    public record PlayerCardSnapshot(String matchId, String cardId, String cardImageUrl) {
    }
}
