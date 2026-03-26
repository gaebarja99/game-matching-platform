package com.gamematcher.service.account;

import com.gamematcher.dto.account.AccountConnectionStatusDto;
import com.gamematcher.dto.account.AccountConnectionsResponseDto;
import com.gamematcher.dto.account.AccountLinkRefreshResponseDto;
import com.gamematcher.dto.account.OAuthStartResponseDto;
import com.gamematcher.dto.profile.ProfilePublicResponseDto;
import com.gamematcher.dto.riot.RiotLeagueEntryResponseDto;
import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.entity.account.BlizzardAccount;
import com.gamematcher.entity.account.DiscordAccount;
import com.gamematcher.entity.account.RiotAccount;
import com.gamematcher.entity.account.SteamAccount;
import com.gamematcher.entity.profile.UserProfile;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.account.BlizzardAccountRepository;
import com.gamematcher.repository.account.DiscordAccountRepository;
import com.gamematcher.repository.account.RiotAccountRepository;
import com.gamematcher.repository.account.SteamAccountRepository;
import com.gamematcher.repository.profile.UserProfileRepository;
import com.gamematcher.service.riot.LolApiService;
import com.gamematcher.service.valorant.ValorantApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountConnectionService {

    private final DiscordAccountRepository discordAccountRepository;
    private final SteamAccountRepository steamAccountRepository;
    private final BlizzardAccountRepository blizzardAccountRepository;
    private final RiotAccountRepository riotAccountRepository;
    private final DiscordAccountService discordAccountService;
    private final SteamAccountService steamAccountService;
    private final BlizzardAccountService blizzardAccountService;
    private final OAuthLinkStateService oAuthLinkStateService;
    private final RestTemplate restTemplate;
    private final LolApiService riotLolApiService;
    private final ValorantApiService valorantApiService;
    private final UserProfileRepository userProfileRepository;
    private final RiotAccountService riotAccountService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${discord.oauth.client-id:}")
    private String discordClientId;

    @Value("${discord.oauth.client-secret:}")
    private String discordClientSecret;

    @Value("${blizzard.oauth.client-id:}")
    private String blizzardClientId;

    @Value("${blizzard.oauth.client-secret:}")
    private String blizzardClientSecret;

    @Value("${steam.api.key:}")
    private String steamApiKey;

    public AccountConnectionsResponseDto getConnections(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return new AccountConnectionsResponseDto(userId, buildAllWithVisibility(userId, profile));
    }

    /**
     * 프로필 응답에 연동 목록을 붙인다. 타인 조회 시 비공개·보조 식별자·안내 문구는 제외한다.
     */
    @Transactional(readOnly = true)
    public void fillConnections(ProfilePublicResponseDto dto, Long userId, boolean ownerView) {
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        List<AccountConnectionStatusDto> all = buildAllWithVisibility(userId, profile);
        if (ownerView) {
            dto.setConnections(all.stream()
                    .filter(AccountConnectionStatusDto::isConnected)
                    .collect(Collectors.toList()));
        } else {
            dto.setConnections(all.stream()
                    .filter(c -> c.isConnected() && c.isPublicProfileVisible())
                    .map(this::sanitizeConnectionForPublic)
                    .collect(Collectors.toList()));
        }
    }

    private List<AccountConnectionStatusDto> buildAllWithVisibility(Long userId, UserProfile profile) {
        List<AccountConnectionStatusDto> connections = new ArrayList<>();
        connections.add(withLinkVisibility(buildDiscordStatus(userId), profile, UserProfile::getPublicDiscordLinkVisible));
        connections.add(withLinkVisibility(buildSteamStatus(userId), profile, UserProfile::getPublicSteamLinkVisible));
        connections.add(withLinkVisibility(buildBlizzardStatus(userId), profile, UserProfile::getPublicBlizzardLinkVisible));
        connections.add(withRiotLinkVisibility(buildRiotStatus(userId), profile));
        return connections;
    }

    private static AccountConnectionStatusDto withRiotLinkVisibility(AccountConnectionStatusDto base, UserProfile profile) {
        boolean linkVis = profile == null || profile.getPublicRiotLinkVisible() == null || profile.getPublicRiotLinkVisible();
        boolean lolVis = profile == null || profile.getPublicRiotLolRankVisible() == null || profile.getPublicRiotLolRankVisible();
        boolean valVis =
                profile == null || profile.getPublicRiotValorantRankVisible() == null || profile.getPublicRiotValorantRankVisible();
        return base.toBuilder()
                .publicProfileVisible(linkVis)
                .publicLolRankVisible(lolVis)
                .publicValorantRankVisible(valVis)
                .build();
    }

    private static AccountConnectionStatusDto withLinkVisibility(
            AccountConnectionStatusDto base,
            UserProfile profile,
            Function<UserProfile, Boolean> visibilityFlag) {
        boolean vis = true;
        if (profile != null) {
            Boolean v = visibilityFlag.apply(profile);
            vis = v == null || v;
        }
        return base.toBuilder().publicProfileVisible(vis).build();
    }

    private AccountConnectionStatusDto sanitizeConnectionForPublic(AccountConnectionStatusDto c) {
        AccountConnectionStatusDto.AccountConnectionStatusDtoBuilder b = c.toBuilder()
                .secondaryValue(null)
                .note(null)
                .connectUrl(null);
        if ("riot".equalsIgnoreCase(c.getProvider())) {
            if (!c.isPublicLolRankVisible()) {
                b.lolRankSummary(null);
            }
            if (!c.isPublicValorantRankVisible()) {
                b.valorantRankSummary(null);
            }
        }
        return b.build();
    }

    /**
     * 연동된 외부 계정 표시 정보·(가능한 경우) 게임 캐시를 최신으로 맞춘다.
     */
    @Transactional
    public AccountLinkRefreshResponseDto refreshLinkedProfile(Long userId, String provider) {
        return switch (provider.toLowerCase()) {
            case "riot" -> refreshRiotLinked(userId);
            case "steam" -> refreshSteamLinked(userId);
            case "discord" -> new AccountLinkRefreshResponseDto(false,
                    "Discord 닉네임·아바타는 OAuth 연동 시점 기준으로 저장됩니다. 변경 후에는 「연동 해제」 후 다시 연결해 주세요.");
            case "blizzard" -> new AccountLinkRefreshResponseDto(false,
                    "Battle.net 표시명은 OAuth 연동 시점 기준입니다. 최신 정보가 필요하면 연동 해제 후 다시 연결해 주세요.");
            default -> throw new GameApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 연동 제공자입니다: " + provider);
        };
    }

    private AccountLinkRefreshResponseDto refreshRiotLinked(Long userId) {
        riotAccountService.refreshLinkedData(userId);
        return new AccountLinkRefreshResponseDto(true, "Riot 닉네임·LoL·발로란트 연동 데이터를 최신으로 불러왔습니다.");
    }

    private AccountLinkRefreshResponseDto refreshSteamLinked(Long userId) {
        SteamAccount acc = steamAccountRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "연동된 Steam 계정이 없습니다."));
        if (steamApiKey == null || steamApiKey.isBlank()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST,
                    "Steam Web API 키(steam.api.key)가 설정되어 있지 않아 프로필을 갱신할 수 없습니다.");
        }
        String steamId = acc.getSteamId();
        String personaName = steamId;
        String avatar = null;
        Map<?, ?> summaries = restTemplate.getForObject(
                "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key="
                        + enc(steamApiKey) + "&steamids=" + enc(steamId),
                Map.class
        );
        Object responseObj = summaries == null ? null : summaries.get("response");
        if (responseObj instanceof Map<?, ?> responseMap) {
            Object playersObj = responseMap.get("players");
            if (playersObj instanceof List<?> players && !players.isEmpty() && players.get(0) instanceof Map<?, ?> player) {
                String pn = stringValue(player.get("personaname"));
                if (pn != null && !pn.isBlank()) {
                    personaName = pn;
                }
                avatar = stringValue(player.get("avatarfull"));
            }
        }
        boolean changed =
                !Objects.equals(acc.getPersonaName(), personaName) || !Objects.equals(acc.getAvatar(), avatar);
        acc.setPersonaName(personaName);
        acc.setAvatar(avatar);
        steamAccountRepository.save(acc);
        return new AccountLinkRefreshResponseDto(changed,
                changed ? "Steam 닉네임·아바타를 최신으로 반영했습니다." : "이미 최신 Steam 프로필입니다.");
    }

    @Transactional
    public void unlink(Long userId, String provider) {
        switch (provider.toLowerCase()) {
            case "discord" -> discordAccountRepository.findFirstByUserId(userId)
                    .ifPresent(discordAccountRepository::delete);
            case "steam" -> steamAccountRepository.findFirstByUserId(userId)
                    .ifPresent(steamAccountRepository::delete);
            case "blizzard" -> blizzardAccountRepository.findFirstByUserId(userId)
                    .ifPresent(blizzardAccountRepository::delete);
            case "riot" -> riotAccountRepository.findFirstByUserId(userId)
                    .ifPresent(riotAccountRepository::delete);
            default -> throw new GameApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 연동 제공자입니다: " + provider);
        }
    }

    public OAuthStartResponseDto startDiscord(Long userId) {
        requireConfigured(discordClientId, "Discord OAuth Client ID");
        requireConfigured(discordClientSecret, "Discord OAuth Client Secret");
        String state = oAuthLinkStateService.createState(userId, "discord");
        String callback = backendBaseUrl + "/api/account-links/oauth/discord/callback";
        String url = "https://discord.com/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + enc(discordClientId)
                + "&scope=" + enc("identify")
                + "&state=" + enc(state)
                + "&redirect_uri=" + enc(callback);
        return new OAuthStartResponseDto("discord", url);
    }

    public OAuthStartResponseDto startBlizzard(Long userId) {
        requireConfigured(blizzardClientId, "Blizzard OAuth Client ID");
        requireConfigured(blizzardClientSecret, "Blizzard OAuth Client Secret");
        String state = oAuthLinkStateService.createState(userId, "blizzard");
        String callback = backendBaseUrl + "/api/account-links/oauth/blizzard/callback";
        String url = "https://oauth.battle.net/authorize"
                + "?response_type=code"
                + "&client_id=" + enc(blizzardClientId)
                + "&scope=" + enc("openid")
                + "&state=" + enc(state)
                + "&redirect_uri=" + enc(callback);
        return new OAuthStartResponseDto("blizzard", url);
    }

    public OAuthStartResponseDto startSteam(Long userId) {
        String state = oAuthLinkStateService.createState(userId, "steam");
        String callback = backendBaseUrl + "/api/account-links/oauth/steam/callback";
        String realm = backendBaseUrl;
        String url = "https://steamcommunity.com/openid/login"
                + "?openid.ns=" + enc("http://specs.openid.net/auth/2.0")
                + "&openid.mode=checkid_setup"
                + "&openid.return_to=" + enc(callback + "?state=" + state)
                + "&openid.realm=" + enc(realm)
                + "&openid.identity=" + enc("http://specs.openid.net/auth/2.0/identifier_select")
                + "&openid.claimed_id=" + enc("http://specs.openid.net/auth/2.0/identifier_select");
        return new OAuthStartResponseDto("steam", url);
    }

    @Transactional
    public String handleDiscordCallback(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            return buildFrontendRedirect("discord", "error", "Discord 연동이 취소되었습니다.");
        }
        Long userId = oAuthLinkStateService.consumeState(state, "discord").getUserId();
        String callback = backendBaseUrl + "/api/account-links/oauth/discord/callback";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", discordClientId);
        body.add("client_secret", discordClientSecret);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", callback);

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<?, ?> tokenResponse = restTemplate.postForObject(
                "https://discord.com/api/oauth2/token",
                new HttpEntity<>(body, tokenHeaders),
                Map.class
        );
        String accessToken = tokenResponse == null ? null : String.valueOf(tokenResponse.get("access_token"));
        if (accessToken == null || accessToken.isBlank()) {
            throw new GameApiException(HttpStatus.BAD_GATEWAY, "Discord access token 발급에 실패했습니다.");
        }

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        Map<?, ?> userResponse = restTemplate.exchange(
                "https://discord.com/api/users/@me",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                Map.class
        ).getBody();
        if (userResponse == null) {
            throw new GameApiException(HttpStatus.BAD_GATEWAY, "Discord 사용자 정보를 가져오지 못했습니다.");
        }

        String discordId = stringValue(userResponse.get("id"));
        String username = buildDiscordUsername(userResponse);
        String avatar = buildDiscordAvatar(userResponse);
        discordAccountService.linkDiscordAccount(userId, discordId, username, avatar);
        return buildFrontendRedirect("discord", "success", "Discord 계정이 연동되었습니다.");
    }

    @Transactional
    public String handleBlizzardCallback(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            return buildFrontendRedirect("blizzard", "error", "Blizzard 연동이 취소되었습니다.");
        }
        Long userId = oAuthLinkStateService.consumeState(state, "blizzard").getUserId();
        String callback = backendBaseUrl + "/api/account-links/oauth/blizzard/callback";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", callback);

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        tokenHeaders.set("Authorization", basicAuth(blizzardClientId, blizzardClientSecret));

        Map<?, ?> tokenResponse = restTemplate.postForObject(
                "https://oauth.battle.net/token",
                new HttpEntity<>(body, tokenHeaders),
                Map.class
        );
        String accessToken = tokenResponse == null ? null : stringValue(tokenResponse.get("access_token"));
        if (accessToken == null || accessToken.isBlank()) {
            throw new GameApiException(HttpStatus.BAD_GATEWAY, "Blizzard access token 발급에 실패했습니다.");
        }

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        Map<?, ?> userResponse = restTemplate.exchange(
                "https://oauth.battle.net/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                Map.class
        ).getBody();
        if (userResponse == null) {
            throw new GameApiException(HttpStatus.BAD_GATEWAY, "Blizzard 사용자 정보를 가져오지 못했습니다.");
        }

        String accountId = stringValue(userResponse.get("sub"));
        String battleTag = stringValue(userResponse.get("battletag"));
        if (battleTag == null || battleTag.isBlank()) {
            battleTag = stringValue(userResponse.get("id"));
        }
        blizzardAccountService.linkBlizzardAccount(userId, battleTag, accountId, "global");
        return buildFrontendRedirect("blizzard", "success", "Blizzard 계정이 연동되었습니다.");
    }

    @Transactional
    public String handleSteamCallback(Map<String, String> params) {
        String state = params.get("state");
        Long userId = oAuthLinkStateService.consumeState(state, "steam").getUserId();
        verifySteamResponse(params);

        String claimedId = params.get("openid.claimed_id");
        String steamId = claimedId == null ? null : claimedId.substring(claimedId.lastIndexOf('/') + 1);
        if (steamId == null || steamId.isBlank()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "Steam ID를 확인하지 못했습니다.");
        }

        String personaName = steamId;
        String avatar = null;
        if (steamApiKey != null && !steamApiKey.isBlank()) {
            Map<?, ?> summaries = restTemplate.getForObject(
                    "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key="
                            + enc(steamApiKey) + "&steamids=" + enc(steamId),
                    Map.class
            );
            Object responseObj = summaries == null ? null : summaries.get("response");
            if (responseObj instanceof Map<?, ?> responseMap) {
                Object playersObj = responseMap.get("players");
                if (playersObj instanceof List<?> players && !players.isEmpty() && players.get(0) instanceof Map<?, ?> player) {
                    personaName = stringValue(player.get("personaname"));
                    avatar = stringValue(player.get("avatarfull"));
                }
            }
        }

        steamAccountService.linkSteamAccount(userId, steamId, personaName, avatar);
        return buildFrontendRedirect("steam", "success", "Steam 계정이 연동되었습니다.");
    }

    private void verifySteamResponse(Map<String, String> params) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if ("state".equals(entry.getKey())) {
                continue;
            }
            body.add(entry.getKey(), entry.getValue());
        }
        body.set("openid.mode", "check_authentication");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String response = restTemplate.postForObject(
                "https://steamcommunity.com/openid/login",
                new HttpEntity<>(body, headers),
                String.class
        );
        if (response == null || !response.contains("is_valid:true")) {
            throw new GameApiException(HttpStatus.UNAUTHORIZED, "Steam OpenID 검증에 실패했습니다.");
        }
    }

    private AccountConnectionStatusDto buildDiscordStatus(Long userId) {
        DiscordAccount account = discordAccountRepository.findFirstByUserId(userId).orElse(null);
        return AccountConnectionStatusDto.builder()
                .provider("discord")
                .connected(account != null)
                .displayName(account == null ? null : account.getUsername())
                .secondaryValue(account == null ? null : account.getDiscordId())
                .avatarUrl(account == null ? null : account.getAvatar())
                .ownershipVerified(account != null)
                .note("Discord OAuth2 본인 인증 기반 연동")
                .build();
    }

    private AccountConnectionStatusDto buildSteamStatus(Long userId) {
        SteamAccount account = steamAccountRepository.findFirstByUserId(userId).orElse(null);
        return AccountConnectionStatusDto.builder()
                .provider("steam")
                .connected(account != null)
                .displayName(account == null ? null : account.getPersonaName())
                .secondaryValue(account == null ? null : account.getSteamId())
                .avatarUrl(account == null ? null : account.getAvatar())
                .ownershipVerified(account != null)
                .note("Steam OpenID 본인 인증 기반 연동")
                .build();
    }

    private AccountConnectionStatusDto buildBlizzardStatus(Long userId) {
        BlizzardAccount account = blizzardAccountRepository.findFirstByUserId(userId).orElse(null);
        return AccountConnectionStatusDto.builder()
                .provider("blizzard")
                .connected(account != null)
                .displayName(account == null ? null : account.getBattleTag())
                .secondaryValue(account == null ? null : account.getAccountId())
                .ownershipVerified(account != null)
                .note("Battle.net OAuth 본인 인증 기반 연동")
                .build();
    }

    private AccountConnectionStatusDto buildRiotStatus(Long userId) {
        RiotAccount account = riotAccountRepository.findFirstByUserId(userId).orElse(null);
        String lolRankSummary = null;
        String valorantRankSummary = null;
        if (account != null) {
            try {
                List<RiotLeagueEntryResponseDto> entries = riotLolApiService.findLeagueEntriesByPuuidOrEmpty(account.getPuuid());
                lolRankSummary = formatLolRankSummary(entries);
            } catch (Exception e) {
                log.debug("LoL 랭크 요약 생략: {}", e.getMessage());
            }
            try {
                ValorantMmrApiResponse mmr = valorantApiService.fetchMmrForRiotLinkedProfile(
                        account.getGameName(), account.getTagLine());
                valorantRankSummary = formatValorantRankSummary(mmr);
            } catch (Exception e) {
                log.debug("발로란트 티어 요약 생략: {}", e.getMessage());
            }
        }
        return AccountConnectionStatusDto.builder()
                .provider("riot")
                .connected(account != null)
                .displayName(account == null ? null : account.getGameName() + "#" + account.getTagLine())
                .secondaryValue(null)
                .ownershipVerified(false)
                .note("현재 Riot은 공개 API 조회 기반 연동만 가능하며, OAuth 기반 소유권 인증은 미구현 상태입니다.")
                .lolRankSummary(lolRankSummary)
                .valorantRankSummary(valorantRankSummary)
                .build();
    }

    private static String formatLolRankSummary(List<RiotLeagueEntryResponseDto> entries) {
        if (entries == null || entries.isEmpty()) {
            return "LoL: 솔로/자유 랭크 없음 (미배치)";
        }
        StringBuilder sb = new StringBuilder();
        for (RiotLeagueEntryResponseDto e : entries) {
            if ("RANKED_SOLO_5x5".equals(e.getQueueType())) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append("LoL 솔로: ").append(formatLolLeagueEntry(e));
            } else if ("RANKED_FLEX_SR".equals(e.getQueueType())) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append("LoL 자유: ").append(formatLolLeagueEntry(e));
            }
        }
        if (sb.length() == 0) {
            return "LoL: 솔로/자유 랭크 없음 (미배치)";
        }
        return sb.toString();
    }

    private static String formatLolLeagueEntry(RiotLeagueEntryResponseDto e) {
        String tier = e.getTier() != null ? e.getTier() : "UNRANKED";
        String div = e.getRank() != null && !e.getRank().isBlank() ? " " + e.getRank() : "";
        return tier + div + " (" + e.getLeaguePoints() + " LP)";
    }

    private static String formatValorantRankSummary(ValorantMmrApiResponse mmr) {
        if (mmr == null || mmr.getData() == null) {
            return null;
        }
        ValorantMmrApiResponse.MmrData data = mmr.getData();
        ValorantMmrApiResponse.CurrentData cd = data.getCurrentData();
        if (cd != null) {
            String patched = cd.getCurrentTierPatched();
            Integer need = cd.getGamesNeededForRating();
            if (patched == null || patched.isBlank()) {
                if (need != null && need > 0) {
                    return "발로란트 경쟁: 배치전 (" + need + "경기 남음)";
                }
            } else {
                if (need != null && need > 0) {
                    return "발로란트 경쟁: " + patched + " (등록 " + need + "경기 남음)";
                }
                return "발로란트 경쟁: " + patched;
            }
        }
        ValorantMmrApiResponse.HighestRank hr = data.getHighestRank();
        if (hr != null && hr.getPatchedTier() != null && !hr.getPatchedTier().isBlank()) {
            return "발로란트 경쟁: 시즌 최고 " + hr.getPatchedTier();
        }
        return "발로란트 경쟁: 경쟁 전적 없음·언랭";
    }

    private void requireConfigured(String value, String label) {
        if (value == null || value.isBlank() || value.contains("YOUR_")) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, label + " 설정이 필요합니다.");
        }
    }

    private String buildFrontendRedirect(String provider, String result, String message) {
        String nextUrl = frontendBaseUrl + "/connections?provider=" + enc(provider)
                + "&result=" + enc(result)
                + "&message=" + enc(message);
        return backendBaseUrl + "/api/account-links/oauth/result?provider=" + enc(provider)
                + "&result=" + enc(result)
                + "&message=" + enc(message)
                + "&next=" + enc(nextUrl);
    }

    private String basicAuth(String clientId, String clientSecret) {
        String raw = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String buildDiscordUsername(Map<?, ?> userResponse) {
        String username = stringValue(userResponse.get("global_name"));
        if (username == null || username.isBlank()) {
            username = stringValue(userResponse.get("username"));
        }
        String discriminator = stringValue(userResponse.get("discriminator"));
        if (discriminator != null && !"0".equals(discriminator) && username != null && !username.contains("#")) {
            username = username + "#" + discriminator;
        }
        return username;
    }

    private String buildDiscordAvatar(Map<?, ?> userResponse) {
        String discordId = stringValue(userResponse.get("id"));
        String avatar = stringValue(userResponse.get("avatar"));
        if (discordId == null || avatar == null || avatar.isBlank()) {
            return null;
        }
        return "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatar + ".png";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
