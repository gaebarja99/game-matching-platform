package com.gamematcher.service.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.account.RiotAccountLinkResponseDto;
import com.gamematcher.dto.account.RiotVerificationConfirmRequestDto;
import com.gamematcher.dto.account.RiotVerificationStartRequestDto;
import com.gamematcher.dto.account.RiotVerificationStartResponseDto;
import com.gamematcher.dto.riot.RiotAccountResponseDto;
import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.service.riot.LolApiService;
import com.gamematcher.service.valorant.ValorantApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RiotOwnershipVerificationService {

    private static final String METHOD_LOL_THIRD_PARTY_CODE = "LOL_THIRD_PARTY_CODE";
    private static final String METHOD_VALORANT_CARD_SWAP = "VALORANT_CARD_SWAP";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String LOL_NOT_FOUND_MESSAGE =
            "이 Riot ID로는 LoL 소환사 정보를 찾을 수 없습니다. 실제로 리그 오브 레전드를 플레이한 계정인지 확인해 주세요.";

    private final LolApiService lolApiService;
    private final ValorantApiService valorantApiService;
    private final RiotAccountService riotAccountService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SecureRandom random = new SecureRandom();
    private final Map<String, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();

    public RiotVerificationStartResponseDto startVerification(Long userId, RiotVerificationStartRequestDto request) {
        String gameType = normalize(request.getGameType());
        String gameName = request.getGameName().trim();
        String tagLine = request.getTagLine().trim();
        String platform = normalize(request.getPlatform());

        return switch (gameType) {
            case "lol" -> startLolVerification(userId, gameType, platform, gameName, tagLine);
            case "valorant" -> startValorantVerification(userId, gameType, platform, gameName, tagLine);
            default -> throw new GameApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 Riot 게임 타입입니다: " + gameType);
        };
    }

    public RiotAccountLinkResponseDto confirmVerification(Long userId, RiotVerificationConfirmRequestDto request) {
        String verificationId = request.getVerificationId().trim();
        PendingVerification pending = pendingVerifications.get(verificationId);
        if (pending == null || !pending.userId().equals(userId)) {
            throw new GameApiException(HttpStatus.NOT_FOUND, "진행 중인 Riot 인증 정보를 찾을 수 없습니다.");
        }

        boolean verified = switch (pending.verificationMethod()) {
            case METHOD_LOL_THIRD_PARTY_CODE -> verifyLolThirdPartyCode(pending);
            case METHOD_VALORANT_CARD_SWAP -> verifyValorantCardSwap(pending);
            default -> false;
        };

        if (!verified) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, pending.failureMessage());
        }

        pendingVerifications.remove(verificationId);
        return riotAccountService.linkVerifiedAccount(
                userId,
                pending.gameType(),
                pending.gameName(),
                pending.tagLine(),
                pending.puuid(),
                pending.verificationMethod()
        );
    }

    private RiotVerificationStartResponseDto startLolVerification(
            Long userId,
            String gameType,
            String platform,
            String gameName,
            String tagLine
    ) {
        if (platform == null || platform.isBlank()) {
            platform = "kr";
        }

        log.info("[RiotVerify][LoL] start userId={} gameName={} tagLine={} platform={}", userId, gameName, tagLine, platform);

        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(gameName, tagLine);
        if (account == null || account.getPuuid() == null || account.getPuuid().isBlank()) {
            log.warn("[RiotVerify][LoL] Riot account lookup returned empty. gameName={} tagLine={}", gameName, tagLine);
            throw new GameApiException(HttpStatus.BAD_GATEWAY, "Riot 계정 정보를 불러오지 못했습니다.");
        }

        log.info("[RiotVerify][LoL] account lookup success puuid={}", account.getPuuid());
        String platformBaseUrl = lolApiService.resolvePlatformBaseUrl(platform);
        log.info("[RiotVerify][LoL] summoner lookup platformBaseUrl={}", platformBaseUrl);

        String raw = null;
        try {
            raw = lolApiService.getSummonerRawByPuuid(account.getPuuid(), platformBaseUrl);
            log.info("[RiotVerify][LoL] summoner raw success platform={} body={}", platform, raw);
        } catch (RuntimeException exception) {
            log.warn(
                    "[RiotVerify][LoL] summoner raw failed platform={} puuid={} message={}",
                    platform,
                    account.getPuuid(),
                    exception.getMessage()
            );
            String message = exception.getMessage();
            if (message != null && message.contains("summoner not found")) {
                raw = null;
            } else {
                throw exception;
            }
        }

        String encryptedSummonerId;
        try {
            encryptedSummonerId = raw == null || raw.isBlank()
                    ? null
                    : objectMapper.readTree(raw).path("id").asText(null);
            if (encryptedSummonerId == null || encryptedSummonerId.isBlank()) {
                encryptedSummonerId = lolApiService.getSummonerIdFromLeagueEntriesByPuuid(account.getPuuid(), platformBaseUrl);
                log.info("[RiotVerify][LoL] fallback summonerId from league entries={}", encryptedSummonerId);
            }
            if (encryptedSummonerId == null || encryptedSummonerId.isBlank()) {
                encryptedSummonerId = lolApiService.getSummonerIdFromRecentMatchesByPuuid(account.getPuuid());
                log.info("[RiotVerify][LoL] fallback summonerId from recent matches={}", encryptedSummonerId);
            }
        } catch (Exception exception) {
            log.error("[RiotVerify][LoL] summoner raw parse failed raw={}", raw, exception);
            throw new GameApiException(HttpStatus.BAD_GATEWAY, "LoL 소환사 정보를 해석하지 못했습니다.");
        }

        log.info("[RiotVerify][LoL] encryptedSummonerId={}", encryptedSummonerId);

        if (encryptedSummonerId == null || encryptedSummonerId.isBlank()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, LOL_NOT_FOUND_MESSAGE);
        }

        String verificationId = UUID.randomUUID().toString();
        String verificationCode = randomVerificationCode();
        pendingVerifications.put(verificationId, new PendingVerification(
                verificationId,
                userId,
                gameType,
                gameName,
                tagLine,
                account.getPuuid(),
                METHOD_LOL_THIRD_PARTY_CODE,
                encryptedSummonerId,
                verificationCode,
                platformBaseUrl,
                null
        ));

        return RiotVerificationStartResponseDto.builder()
                .verificationId(verificationId)
                .gameType(gameType)
                .platform(platform)
                .gameName(gameName)
                .tagLine(tagLine)
                .verificationMethod(METHOD_LOL_THIRD_PARTY_CODE)
                .verificationCode(verificationCode)
                .instructionTitle("LoL 제3자 확인 코드 인증")
                .instructionBody("게임 클라이언트에서 설정 > 계정 > 제3자 확인 코드에 아래 코드를 입력하고 저장한 뒤 인증 완료 버튼을 눌러 주세요.")
                .build();
    }

    private RiotVerificationStartResponseDto startValorantVerification(
            Long userId,
            String gameType,
            String region,
            String gameName,
            String tagLine
    ) {
        if (region == null || region.isBlank()) {
            region = "kr";
        }

        log.info("[RiotVerify][Valorant] start userId={} gameName={} tagLine={} region={}",
                userId, gameName, tagLine, region);

        RiotAccountResponseDto riotAccount = lolApiService.getAccountByRiotId(gameName, tagLine);
        if (riotAccount == null || riotAccount.getPuuid() == null || riotAccount.getPuuid().isBlank()) {
            throw new GameApiException(HttpStatus.BAD_GATEWAY, "Riot 계정 정보를 불러오지 못했습니다.");
        }
        log.info("[RiotVerify][Valorant] riot account lookup success puuid={}", riotAccount.getPuuid());

        ValorantPuuidApiResponse accountResponse = null;
        try {
            accountResponse = valorantApiService.getAccountByPuuid(riotAccount.getPuuid());
        } catch (RuntimeException exception) {
            log.warn("[RiotVerify][Valorant] account-by-puuid lookup failed puuid={} region={} message={}",
                    riotAccount.getPuuid(), region, exception.getMessage());
            try {
                accountResponse = valorantApiService.getAccountByNameTag(gameName, tagLine);
                log.info("[RiotVerify][Valorant] fallback name-tag lookup success puuid={}",
                        accountResponse != null && accountResponse.getData() != null ? accountResponse.getData().getPuuid() : null);
            } catch (RuntimeException fallbackError) {
                log.warn("[RiotVerify][Valorant] fallback name-tag lookup failed gameName={} tagLine={} region={} message={}",
                        gameName, tagLine, region, fallbackError.getMessage());
            }
        }

        ValorantPuuidApiResponse.AccountData account = accountResponse != null ? accountResponse.getData() : null;
        if (account != null && account.getPuuid() != null && !account.getPuuid().isBlank()) {
            log.info("[RiotVerify][Valorant] account lookup success puuid={} apiRegion={}",
                    account.getPuuid(), account.getRegion());
        }

        String currentCardId = account != null && account.getCard() != null ? account.getCard().getId() : null;
        String currentCardImageUrl = account != null && account.getCard() != null ? account.getCard().getSmall() : null;
        boolean recentMatchFallback = false;

        if (currentCardId == null || currentCardId.isBlank()) {
            try {
                ValorantApiService.PlayerCardSnapshot snapshot =
                        valorantApiService.getLatestPlayerCardFromRecentMatches(region, riotAccount.getPuuid());
                if (snapshot != null && snapshot.cardId() != null && !snapshot.cardId().isBlank()) {
                    currentCardId = snapshot.cardId();
                    currentCardImageUrl = snapshot.cardImageUrl();
                    recentMatchFallback = true;
                    log.info("[RiotVerify][Valorant] fallback recent-match card success puuid={} matchId={} cardId={}",
                            riotAccount.getPuuid(), snapshot.matchId(), snapshot.cardId());
                }
            } catch (RuntimeException matchFallbackError) {
                log.warn("[RiotVerify][Valorant] fallback recent-match card failed puuid={} region={} message={}",
                        riotAccount.getPuuid(), region, matchFallbackError.getMessage());
            }
        }

        if (currentCardId == null || currentCardId.isBlank()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST,
                    "Valorant 계정은 확인했지만 현재 카드 정보를 조회하지 못했습니다. 최근 한 판 플레이한 뒤 다시 시도해 주세요.");
        }

        String verificationId = UUID.randomUUID().toString();
        pendingVerifications.put(verificationId, new PendingVerification(
                verificationId,
                userId,
                gameType,
                gameName,
                tagLine,
                riotAccount.getPuuid(),
                METHOD_VALORANT_CARD_SWAP,
                null,
                null,
                region,
                currentCardId
        ));

        return RiotVerificationStartResponseDto.builder()
                .verificationId(verificationId)
                .gameType(gameType)
                .platform(region)
                .gameName(gameName)
                .tagLine(tagLine)
                .verificationMethod(METHOD_VALORANT_CARD_SWAP)
                .currentCardId(currentCardId)
                .currentCardImageUrl(currentCardImageUrl)
                .instructionTitle("Valorant 카드 변경 인증")
                .instructionBody(recentMatchFallback
                        ? "최근 경기에서 확인된 카드 기준으로 인증을 진행합니다. 다른 카드로 변경한 뒤 인증 완료 버튼을 눌러 주세요."
                        : "현재 플레이어 카드와 다른 카드로 변경한 뒤 인증 완료 버튼을 눌러 주세요. 변경 후 데이터 반영까지 시간이 조금 걸릴 수 있습니다.")
                .build();
    }

    private boolean verifyLolThirdPartyCode(PendingVerification pending) {
        String apiCode = lolApiService.getThirdPartyCodeBySummonerId(
                pending.lolSummonerId(),
                pending.lolPlatformBaseUrl()
        );
        return apiCode != null && apiCode.trim().equalsIgnoreCase(pending.verificationCode());
    }

    private boolean verifyValorantCardSwap(PendingVerification pending) {
        ValorantPuuidApiResponse accountResponse = null;
        try {
            accountResponse = valorantApiService.getAccountByPuuid(pending.puuid());
        } catch (RuntimeException exception) {
            log.warn("[RiotVerify][Valorant] confirm account-by-puuid lookup failed puuid={} region={} message={}",
                    pending.puuid(), pending.lolPlatformBaseUrl(), exception.getMessage());
            try {
                accountResponse = valorantApiService.getAccountByNameTag(pending.gameName(), pending.tagLine());
            } catch (RuntimeException fallbackError) {
                log.warn("[RiotVerify][Valorant] confirm fallback name-tag lookup failed gameName={} tagLine={} region={} message={}",
                        pending.gameName(), pending.tagLine(), pending.lolPlatformBaseUrl(), fallbackError.getMessage());
            }
        }

        ValorantPuuidApiResponse.AccountData account = accountResponse != null ? accountResponse.getData() : null;
        String latestCardId = account != null && account.getCard() != null ? account.getCard().getId() : null;

        if ((latestCardId == null || latestCardId.isBlank())
                && pending.lolPlatformBaseUrl() != null
                && !pending.lolPlatformBaseUrl().isBlank()) {
            try {
                ValorantApiService.PlayerCardSnapshot snapshot =
                        valorantApiService.getLatestPlayerCardFromRecentMatches(pending.lolPlatformBaseUrl(), pending.puuid());
                latestCardId = snapshot != null ? snapshot.cardId() : null;
            } catch (RuntimeException matchFallbackError) {
                log.warn("[RiotVerify][Valorant] confirm recent-match card fallback failed puuid={} region={} message={}",
                        pending.puuid(), pending.lolPlatformBaseUrl(), matchFallbackError.getMessage());
            }
        }

        return latestCardId != null
                && !latestCardId.isBlank()
                && pending.valorantCardId() != null
                && !latestCardId.equalsIgnoreCase(pending.valorantCardId());
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private String randomVerificationCode() {
        StringBuilder builder = new StringBuilder("GM-");
        for (int index = 0; index < 8; index++) {
            builder.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }

    private record PendingVerification(
            String verificationId,
            Long userId,
            String gameType,
            String gameName,
            String tagLine,
            String puuid,
            String verificationMethod,
            String lolSummonerId,
            String verificationCode,
            String lolPlatformBaseUrl,
            String valorantCardId
    ) {
        String failureMessage() {
            return switch (verificationMethod) {
                case METHOD_LOL_THIRD_PARTY_CODE ->
                        "LoL 제3자 확인 코드 인증에 실패했습니다. 게임 클라이언트에 코드를 저장했는지 다시 확인해 주세요.";
                case METHOD_VALORANT_CARD_SWAP ->
                        "Valorant 카드 변경 인증에 실패했습니다. 카드 변경 후 잠시 기다렸다가 다시 시도해 주세요.";
                default -> "Riot 인증 확인에 실패했습니다.";
            };
        }
    }
}
