package com.gamematcher.mapper;

import com.gamematcher.dto.pubg.PubgMatchApiResponse;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import com.gamematcher.service.pubg.PubgJsonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PubgMatchMapper DTO→Entity 변환 테스트")
class PubgMatchMapperTest {

    private final PubgJsonService jsonService = new PubgJsonService();
    private final PubgMatchMapper mapper = new PubgMatchMapper();

    @Test
    @DisplayName("PubgMatchApiResponse를 PubgMatch 엔티티로 변환한다")
    void toEntity_매치API응답_엔티티변환() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_match_sample.json"));
        PubgMatchApiResponse response = jsonService.parseMatchResponse(json);

        PubgMatch match = mapper.toEntity(response);

        assertThat(match).isNotNull();
        assertThat(match.getMatchId()).isEqualTo("a9b6c96f-eea9-4b9e-9195-39272dae2314");
        assertThat(match.getGameMode()).isEqualTo("squad-fpp");
        assertThat(match.getMapName()).isEqualTo("Neon_Main");
        assertThat(match.getDuration()).isEqualTo(1665);

        assertThat(match.getParticipants()).hasSize(60);

        PubgMatchParticipant first = match.getParticipants().stream()
                .filter(p -> "8ink-".equals(p.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(first.getKills()).isEqualTo(4);
        assertThat(first.getDamageDealt()).isEqualTo(853.1866);
        assertThat(first.getWinPlace()).isEqualTo(1);
        assertThat(first.isWin()).isTrue();
        assertThat(first.getPlayerId()).isEqualTo("account.fe1027e418594343bafd39e9685239e2");
    }

    @Test
    @DisplayName("null 응답은 null을 반환한다")
    void toEntity_null_null반환() {
        assertThat(mapper.toEntity((PubgMatchApiResponse) null)).isNull();
    }
}
