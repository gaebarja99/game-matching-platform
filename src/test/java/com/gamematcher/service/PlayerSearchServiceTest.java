package com.gamematcher.service;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.service.apex.ApexApiService;
import com.gamematcher.service.blizzard.BlizzardApiService;
import com.gamematcher.service.cs2.Cs2ApiService;
import com.gamematcher.service.lol.LolApiService;
import com.gamematcher.service.overwatch.OverwatchApiService;
import com.gamematcher.service.pubg.PubgApiService;
import com.gamematcher.service.steam.SteamApiService;
import com.gamematcher.service.tft.TftApiService;
import com.gamematcher.service.valorant.ValorantApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PlayerSearchService 리팩터링 검증
 * SearchService → ApiService 통합 후 각 게임별 라우팅 동작 확인
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerSearchServiceTest {

    @Mock LolApiService lolApiService;
    @Mock TftApiService tftApiService;
    @Mock ValorantApiService valorantApiService;
    @Mock SteamApiService steamApiService;
    @Mock BlizzardApiService blizzardApiService;
    @Mock PubgApiService pubgApiService;
    @Mock OverwatchApiService overwatchApiService;
    @Mock Cs2ApiService cs2ApiService;
    @Mock ApexApiService apexApiService;

    @InjectMocks
    PlayerSearchService playerSearchService;

    private PlayerSearchResponse mockSuccessResponse(String game) {
        return PlayerSearchResponse.builder()
                .success(true)
                .game(game)
                .nickname("test#tag")
                .build();
    }

    @BeforeEach
    void setUp() {
        when(lolApiService.search(any())).thenReturn(mockSuccessResponse("lol"));
        when(tftApiService.search(any())).thenReturn(mockSuccessResponse("tft"));
        when(valorantApiService.search(any())).thenReturn(mockSuccessResponse("valorant"));
        when(steamApiService.search(any())).thenReturn(mockSuccessResponse("steam"));
        when(blizzardApiService.search(any())).thenReturn(mockSuccessResponse("blizzard"));
        when(pubgApiService.search(any())).thenReturn(mockSuccessResponse("pubg"));
        when(overwatchApiService.search(any())).thenReturn(mockSuccessResponse("overwatch"));
        when(cs2ApiService.search(any())).thenReturn(mockSuccessResponse("cs2"));
        when(apexApiService.search(any())).thenReturn(mockSuccessResponse("apex"));
    }

    @Nested
    @DisplayName("게임별 ApiService 라우팅 검증")
    class GameRoutingTest {

        @Test
        void lol_검색시_LolApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("lol");
            req.setGameName("test");
            req.setTagLine("KR1");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(lolApiService).search(any());
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getGame()).isEqualTo("lol");
        }

        @Test
        void tft_검색시_TftApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("tft");
            req.setGameName("test");
            req.setTagLine("KR1");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(tftApiService).search(any());
            assertThat(response.getGame()).isEqualTo("tft");
        }

        @Test
        void valorant_검색시_ValorantApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("valorant");
            req.setGameName("test");
            req.setTagLine("KR1");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(valorantApiService).search(any());
            assertThat(response.getGame()).isEqualTo("valorant");
        }

        @Test
        void steam_검색시_SteamApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("steam");
            req.setSteamId("76561198000000000");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(steamApiService).search(any());
            assertThat(response.getGame()).isEqualTo("steam");
        }

        @Test
        void blizzard_검색시_BlizzardApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("blizzard");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(blizzardApiService).search(any());
            assertThat(response.getGame()).isEqualTo("blizzard");
        }

        @Test
        void pubg_검색시_PubgApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("pubg");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(pubgApiService).search(any());
            assertThat(response.getGame()).isEqualTo("pubg");
        }

        @Test
        void overwatch_검색시_OverwatchApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("overwatch");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(overwatchApiService).search(any());
            assertThat(response.getGame()).isEqualTo("overwatch");
        }

        @Test
        void cs2_검색시_Cs2ApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("cs2");
            req.setGameName("76561198000000000");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(cs2ApiService).search(any());
            assertThat(response.getGame()).isEqualTo("cs2");
        }

        @Test
        void apex_검색시_ApexApiService_호출() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("apex");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(apexApiService).search(any());
            assertThat(response.getGame()).isEqualTo("apex");
        }
    }

    @Nested
    @DisplayName("지원하지 않는 게임")
    class UnsupportedGameTest {

        @Test
        void 미지원_게임_검색시_에러_응답() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("unknown_game");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorMessage()).contains("지원하지 않는 게임");
        }
    }
}
