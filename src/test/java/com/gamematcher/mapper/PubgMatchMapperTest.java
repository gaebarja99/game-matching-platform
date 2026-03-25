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
        assertThat(match.getMatchId()).isEqualTo("04192032-7e46-4d3a-a430-28817c8c5bcc");
        assertThat(match.getGameMode()).isEqualTo("tdm");
        assertThat(match.getMapName()).isEqualTo("Tiger_Main");
        assertThat(match.getDuration()).isEqualTo(303);

        assertThat(match.getParticipants()).hasSize(2);

        PubgMatchParticipant first = match.getParticipants().stream()
                .filter(p -> "HeZ1HeZ1_-".equals(p.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(first.getKills()).isEqualTo(11);
        assertThat(first.getDamageDealt()).isEqualTo(1223.7267);
        assertThat(first.getWinPlace()).isEqualTo(1);
        assertThat(first.isWin()).isTrue();
        assertThat(first.getPlayerId()).isEqualTo("account.a87ceb7c124d486a95375069d712f7b8");
    }

    @Test
    @DisplayName("null 응답은 null을 반환한다")
    void toEntity_null_null반환() {
        assertThat(mapper.toEntity((PubgMatchApiResponse) null)).isNull();
    }
}
