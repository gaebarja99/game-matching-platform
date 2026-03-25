package com.gamematcher.service.account;

import com.gamematcher.dto.account.AccountConnectionStatusDto;
import com.gamematcher.dto.account.AccountConnectionsResponseDto;
import com.gamematcher.dto.account.OAuthStartResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.entity.account.BlizzardAccount;
import com.gamematcher.entity.account.DiscordAccount;
import com.gamematcher.entity.account.RiotAccount;
import com.gamematcher.entity.account.SteamAccount;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.account.BlizzardAccountRepository;
import com.gamematcher.repository.account.DiscordAccountRepository;
import com.gamematcher.repository.account.RiotAccountRepository;
import com.gamematcher.repository.account.SteamAccountRepository;
import com.gamematcher.service.auth.CurrentUserService;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class AccountConnectionService {

    private final CurrentUserService currentUserService;
    private final DiscordAccountRepository discordAccountRepository;
    private final SteamAccountRepository steamAccountRepository;
    private final BlizzardAccountRepository blizzardAccountRepository;
    private final RiotAccountRepository riotAccountRepository;
    private final DiscordAccountService discordAccountService;
    private final SteamAccountService steamAccountService;
    private final BlizzardAccountService blizzardAccountService;
    private final OAuthLinkStateService oAuthLinkStateService;
    private final RestTemplate restTemplate;

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

    public AccountConnectionsResponseDto getConnections(String authToken) {
        User user = currentUserService.requireUser(authToken);
        List<AccountConnectionStatusDto> connections = new ArrayList<>();
        connections.add(buildDiscordStatus(user.getId()));
        connections.add(buildSteamStatus(user.getId()));
        connections.add(buildBlizzardStatus(user.getId()));
        connections.add(buildRiotStatus(user.getId()));
        return new AccountConnectionsResponseDto(user.getId(), connections);
    }

    @Transactional
    public void unlink(String authToken, String provider) {
        User user = currentUserService.requireUser(authToken);
        switch (provider.toLowerCase()) {
            case "discord" -> discordAccountRepository.findFirstByUserId(user.getId())
                    .ifPresent(discordAccountRepository::delete);
            case "steam" -> steamAccountRepository.findFirstByUserId(user.getId())
                    .ifPresent(steamAccountRepository::delete);
            case "blizzard" -> blizzardAccountRepository.findFirstByUserId(user.getId())
                    .ifPresent(blizzardAccountRepository::delete);
            case "riot" -> riotAccountRepository.findFirstByUserId(user.getId())
                    .ifPresent(riotAccountRepository::delete);
            default -> throw new GameApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 연동 제공자입니다: " + provider);
        }
    }

    public OAuthStartResponseDto startDiscord(String authToken) {
        User user = currentUserService.requireUser(authToken);
        requireConfigured(discordClientId, "Discord OAuth Client ID");
        requireConfigured(discordClientSecret, "Discord OAuth Client Secret");
        String state = oAuthLinkStateService.createState(user.getId(), "discord");
        String callback = backendBaseUrl + "/api/account-links/oauth/discord/callback";
        String url = "https://discord.com/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + enc(discordClientId)
                + "&scope=" + enc("identify")
                + "&state=" + enc(state)
                + "&redirect_uri=" + enc(callback);
        return new OAuthStartResponseDto("discord", url);
    }

    public OAuthStartResponseDto startBlizzard(String authToken) {
        User user = currentUserService.requireUser(authToken);
        requireConfigured(blizzardClientId, "Blizzard OAuth Client ID");
        requireConfigured(blizzardClientSecret, "Blizzard OAuth Client Secret");
        String state = oAuthLinkStateService.createState(user.getId(), "blizzard");
        String callback = backendBaseUrl + "/api/account-links/oauth/blizzard/callback";
        String url = "https://oauth.battle.net/authorize"
                + "?response_type=code"
                + "&client_id=" + enc(blizzardClientId)
                + "&scope=" + enc("openid")
                + "&state=" + enc(state)
                + "&redirect_uri=" + enc(callback);
        return new OAuthStartResponseDto("blizzard", url);
    }

    public OAuthStartResponseDto startSteam(String authToken) {
        User user = currentUserService.requireUser(authToken);
        String state = oAuthLinkStateService.createState(user.getId(), "steam");
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
        return AccountConnectionStatusDto.builder()
                .provider("riot")
                .connected(account != null)
                .displayName(account == null ? null : account.getGameName() + "#" + account.getTagLine())
                .secondaryValue(account == null ? null : account.getPuuid())
                .ownershipVerified(false)
                .note("현재 Riot은 공개 API 조회 기반 연동만 가능하며, OAuth 기반 소유권 인증은 미구현 상태입니다.")
                .build();
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
