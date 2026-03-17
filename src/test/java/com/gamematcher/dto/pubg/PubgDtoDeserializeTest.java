package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PUBG DTO 역직렬화 테스트")
class PubgDtoDeserializeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("매치 상세 JSON을 PubgMatchApiResponse로 파싱한다")
    void pubgMatchJson_역직렬화_성공() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_match_sample.json"));
        PubgMatchApiResponse response = objectMapper.readValue(json, PubgMatchApiResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo("04192032-7e46-4d3a-a430-28817c8c5bcc");
        assertThat(response.getData().getType()).isEqualTo("match");

        PubgMatchAttributesDto attrs = response.getData().getAttributes();
        assertThat(attrs).isNotNull();
        assertThat(attrs.getGameMode()).isEqualTo("tdm");
        assertThat(attrs.getMapName()).isEqualTo("Tiger_Main");
        assertThat(attrs.getDuration()).isEqualTo(303);
        assertThat(attrs.getCreatedAt()).isEqualTo("2026-03-16T15:42:19Z");

        assertThat(response.getIncluded()).isNotEmpty();
        List<PubgIncludedItemDto> included = response.getIncluded();

        // participant 스탯 검증 (DBNOs, damageDealt 등)
        PubgParticipantIncludedDto participant = included.stream()
                .filter(PubgParticipantIncludedDto.class::isInstance)
                .map(PubgParticipantIncludedDto.class::cast)
                .filter(p -> p.getAttributes() != null && p.getAttributes().getStats() != null)
                .filter(p -> "HeZ1HeZ1_-".equals(p.getAttributes().getStats().getName()))
                .findFirst()
                .orElseThrow();
        PubgParticipantStatsDto stats = participant.getAttributes().getStats();
        assertThat(stats.getKills()).isEqualTo(11);
        assertThat(stats.getDBNOs()).isEqualTo(0);
        assertThat(stats.getDamageDealt()).isEqualTo(1223.7267);
        assertThat(stats.getWinPlace()).isEqualTo(1);
        assertThat(stats.getPlayerId()).isEqualTo("account.a87ceb7c124d486a95375069d712f7b8");

        // roster 검증
        PubgRosterIncludedDto roster = included.stream()
                .filter(PubgRosterIncludedDto.class::isInstance)
                .map(PubgRosterIncludedDto.class::cast)
                .filter(r -> r.getAttributes() != null && "true".equals(r.getAttributes().getWon()))
                .findFirst()
                .orElseThrow();
        assertThat(roster.getAttributes().getStats().getRank()).isEqualTo(1);

        // asset (telemetry URL) 검증
        PubgAssetIncludedDto asset = included.stream()
                .filter(PubgAssetIncludedDto.class::isInstance)
                .map(PubgAssetIncludedDto.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(asset.getAttributes().getUrl()).contains("telemetry");
    }

    @Test
    @DisplayName("플레이어 조회 JSON을 PubgPlayerApiResponse로 파싱한다")
    void pubgPlayerJson_역직렬화_성공() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_player_sample.json"));
        PubgPlayerApiResponse response = objectMapper.readValue(json, PubgPlayerApiResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotEmpty();

        PubgPlayerDataDto player = response.getData().get(0);
        assertThat(player.getId()).isEqualTo("account.fe1027e418594343bafd39e9685239e2");
        assertThat(player.getType()).isEqualTo("player");
        assertThat(player.getAttributes().getName()).isEqualTo("8ink-");
        assertThat(player.getAttributes().getShardId()).isEqualTo("steam");

        List<PubgResourceIdentifierDto> matchIds = player.getRelationships().getMatchIds();
        assertThat(matchIds).isNotEmpty();
        assertThat(matchIds.get(0).getId()).isEqualTo("04192032-7e46-4d3a-a430-28817c8c5bcc");
    }
}
