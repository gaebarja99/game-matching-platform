package com.gamematcher.controller.account;

import com.gamematcher.dto.account.RiotAccountLinkRequestDto;
import com.gamematcher.dto.account.RiotAccountLinkResponseDto;
import com.gamematcher.dto.riot.*;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.service.account.RiotAccountService;
import com.gamematcher.service.lol.LolApiService;
import com.gamematcher.service.tft.TftApiService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/riot")
public class RiotController {

    private static final Logger log = LoggerFactory.getLogger(RiotController.class);

    private final LolApiService lolApiService;
    private final RiotAccountService riotAccountService;
    private final TftApiService tftApiService;

    public RiotController(LolApiService lolApiService, RiotAccountService riotAccountService, TftApiService tftApiService) {
        this.lolApiService = lolApiService;
        this.riotAccountService = riotAccountService;
        this.tftApiService = tftApiService;
    }

    /** Riot 계정 조회 (gameName + tagLine → puuid) */
    @PostMapping("/account")
    public RiotAccountResponseDto getAccount(@RequestBody RiotAccountRequestDto request) {
        return lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
    }

    /** Riot 계정 DB 저장 (user_id + gameName + tagLine → puuid 저장) */
    @PostMapping("/account/link")
    public RiotAccountLinkResponseDto linkAccount(@Valid @RequestBody RiotAccountLinkRequestDto request) {
        return riotAccountService.linkAccount(request);
    }

    @PostMapping("/summoner")
    public RiotSummonerResponseDto getSummoner(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        return lolApiService.getSummonerByPuuid(account.getPuuid());
    }

    @PostMapping("/summoner/raw")
    public RiotSummonerResponseDto getSummonerRaw(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        RiotSummonerResponseDto summoner = lolApiService.getSummonerByPuuid(account.getPuuid());
        log.debug("summoner id={}, accountId={}, puuid={}, level={}",
                summoner.getId(), summoner.getAccountId(), summoner.getPuuid(), summoner.getSummonerLevel());
        return summoner;
    }

    @PostMapping("/summoner/raw-json")
    public String getSummonerRawJson(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        return lolApiService.getSummonerRawByPuuid(account.getPuuid());
    }

    @PostMapping("/profile")
    public RiotProfileResponseDto getProfile(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        RiotSummonerResponseDto summoner = lolApiService.getSummonerByPuuid(account.getPuuid());
        RiotProfileResponseDto response = new RiotProfileResponseDto();
        response.setPuuid(account.getPuuid());
        response.setGameName(account.getGameName());
        response.setTagLine(account.getTagLine());
        response.setSummonerLevel(summoner.getSummonerLevel());
        response.setProfileIconId(summoner.getProfileIconId());
        return response;
    }

    @PostMapping("/matches")
    public List<String> getMatches(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        return lolApiService.getMatchIdsByPuuid(account.getPuuid(), 0, 5);
    }

    @PostMapping("/match/raw")
    public String getMatchRaw(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        List<String> matchIds = lolApiService.getMatchIdsByPuuid(account.getPuuid(), 0, 1);
        if (matchIds == null || matchIds.isEmpty()) {
            throw new GameApiException(HttpStatus.NOT_FOUND, "최근 매치가 없습니다.");
        }
        return lolApiService.getMatchRawByMatchId(matchIds.get(0));
    }

    @PostMapping("/match/detail")
    public RiotMatchDetailResponseDto getMatchDetail(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        List<String> matchIds = lolApiService.getMatchIdsByPuuid(account.getPuuid(), 0, 1);
        if (matchIds == null || matchIds.isEmpty()) {
            throw new GameApiException(HttpStatus.NOT_FOUND, "최근 매치가 없습니다.");
        }
        return lolApiService.getMatchDetailByMatchId(matchIds.get(0), account.getPuuid());
    }

    @PostMapping("/matches/detail")
    public RiotRecentMatchesResponseDto getRecentMatchDetails(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        List<RiotMatchDetailResponseDto> matches = lolApiService.getRecentMatchDetailsByPuuid(account.getPuuid(), 5);
        RiotRecentMatchesResponseDto response = new RiotRecentMatchesResponseDto();
        response.setPuuid(account.getPuuid());
        response.setGameName(account.getGameName());
        response.setTagLine(account.getTagLine());
        response.setMatches(matches);
        return response;
    }

    @PostMapping("/stats")
    public RiotStatsResponseDto getStats(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        return lolApiService.getRecentStats(
                account.getPuuid(),
                account.getGameName(),
                account.getTagLine()
        );
    }

    /** LoL 최근 5경기 DB 동기화 */
    @PostMapping("/sync")
    public String syncRecentMatches(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        lolApiService.syncRecentMatches(account.getPuuid());
        return "LoL 최근 경기 DB 저장 완료";
    }

    /** TFT 최근 5경기 DB 동기화 */
    @PostMapping("/tft/sync")
    public String syncTftRecentMatches(@RequestBody RiotAccountRequestDto request) {
        RiotAccountResponseDto account = lolApiService.getAccountByRiotId(
                request.getGameName(),
                request.getTagLine()
        );
        tftApiService.syncTftRecentMatches(account.getPuuid(), 5);
        return "TFT 최근 경기 DB 저장 완료";
    }
}
