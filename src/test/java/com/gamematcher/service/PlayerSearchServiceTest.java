package com.gamematcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import com.gamematcher.service.search.Cs2SearchService;
import com.gamematcher.service.search.LolSearchService;
import com.gamematcher.service.search.OverwatchSearchService;
import com.gamematcher.service.search.PubgSearchService;
import com.gamematcher.service.search.TftSearchService;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerSearchServiceTest {

    @Mock LolSearchService lolSearchService;
    @Mock TftSearchService tftSearchService;
    @Mock ValorantApiService valorantApiService;
    @Mock ObjectMapper objectMapper;
    @Mock PubgSearchService pubgSearchService;
    @Mock OverwatchSearchService overwatchSearchService;
    @Mock Cs2SearchService cs2SearchService;

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
        when(lolSearchService.search(any())).thenReturn(mockSuccessResponse("lol"));
        when(tftSearchService.search(any())).thenReturn(mockSuccessResponse("tft"));
        when(valorantApiService.search(any())).thenReturn(mockSuccessResponse("valorant"));
        when(pubgSearchService.search(any())).thenReturn(mockSuccessResponse("pubg"));
        when(overwatchSearchService.search(any())).thenReturn(mockSuccessResponse("overwatch"));
        when(cs2SearchService.search(any())).thenReturn(mockSuccessResponse("cs2"));
    }

    @Nested
    @DisplayName("Game routing")
    class GameRoutingTest {

        @Test
        void routes_lol_requests() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("lol");
            req.setGameName("test");
            req.setTagLine("KR1");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(lolSearchService).search(any());
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getGame()).isEqualTo("lol");
        }

        @Test
        void routes_tft_requests() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("tft");
            req.setGameName("test");
            req.setTagLine("KR1");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(tftSearchService).search(any());
            assertThat(response.getGame()).isEqualTo("tft");
        }

        @Test
        void routes_valorant_requests() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("valorant");
            req.setGameName("test");
            req.setTagLine("KR1");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(valorantApiService).search(any());
            assertThat(response.getGame()).isEqualTo("valorant");
        }

        @Test
        void routes_pubg_requests() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("pubg");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(pubgSearchService).search(any());
            assertThat(response.getGame()).isEqualTo("pubg");
        }

        @Test
        void routes_overwatch_requests() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("overwatch");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(overwatchSearchService).search(any());
            assertThat(response.getGame()).isEqualTo("overwatch");
        }

        @Test
        void routes_cs2_requests() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("cs2");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            verify(cs2SearchService).search(any());
            assertThat(response.getGame()).isEqualTo("cs2");
        }
    }

    @Nested
    @DisplayName("Unsupported game")
    class UnsupportedGameTest {

        @Test
        void returns_error_for_unknown_game() {
            PlayerSearchRequest req = new PlayerSearchRequest();
            req.setGame("unknown_game");
            req.setGameName("test");

            PlayerSearchResponse response = playerSearchService.searchPlayer(req);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorMessage()).contains("지원하지 않는 게임");
        }
    }
}
