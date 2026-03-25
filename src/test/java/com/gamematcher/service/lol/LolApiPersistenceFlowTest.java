package com.gamematcher.service.lol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.lol.*;
import com.gamematcher.entity.account.LolAccount;
import com.gamematcher.entity.account.LolSummonerProfile;
import com.gamematcher.entity.match.lol.LolMatch;
import com.gamematcher.entity.match.lol.LolMatchTimeline;
import com.gamematcher.repository.account.LolAccountRepository;
import com.gamematcher.repository.account.LolSummonerProfileRepository;
import com.gamematcher.repository.match.LolMatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoL API 응답 DTO → 엔티티 저장 흐름 테스트
 */
@SpringBootTest
@Transactional
class LolApiPersistenceFlowTest {

    @Autowired
    private LolApiPersistenceService persistenceService;
    @Autowired
    private LolMatchJsonService lolMatchJsonService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LolAccountRepository lolAccountRepository;
    @Autowired
    private LolSummonerProfileRepository lolSummonerProfileRepository;
    @Autowired
    private LolMatchRepository lolMatchRepository;

    @Test
    @DisplayName("LolAccountResponseDto → LolAccount 저장")
    void saveAccount_fromDto() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/lol/lol_puuid_sample.json"));
        LolAccountResponseDto dto = objectMapper.readValue(json, LolAccountResponseDto.class);

        LolAccount saved = persistenceService.saveAccount(dto);

        assertThat(saved).isNotNull();
        assertThat(saved.getPuuid()).isEqualTo(dto.getPuuid());
        assertThat(saved.getGameName()).isEqualTo(dto.getGameName());
        assertThat(saved.getTagLine()).isEqualTo(dto.getTagLine());
        assertThat(lolAccountRepository.findByPuuid(dto.getPuuid())).isPresent();
    }

    @Test
    @DisplayName("LolSummonerProfileDto → LolSummonerProfile 저장")
    void saveSummonerProfile_fromDto() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/lol/lol_profile_sample.json"));
        LolSummonerProfileDto dto = objectMapper.readValue(json, LolSummonerProfileDto.class);

        LolSummonerProfile saved = persistenceService.saveSummonerProfile(dto);

        assertThat(saved).isNotNull();
        assertThat(saved.getPuuid()).isEqualTo(dto.getPuuid());
        assertThat(saved.getProfileIconId()).isEqualTo(dto.getProfileIconId());
        assertThat(saved.getSummonerLevel()).isEqualTo(dto.getSummonerLevel());
        assertThat(lolSummonerProfileRepository.findByPuuid(dto.getPuuid())).isPresent();
    }

    @Test
    @DisplayName("LolMatchDetailDto → LolMatch 저장")
    void saveMatch_fromDto() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
        LolMatchDetailDto dto = lolMatchJsonService.parseMatchDetail(json);

        LolMatch saved = persistenceService.saveMatch(dto);

        assertThat(saved).isNotNull();
        assertThat(saved.getMatchId()).isEqualTo(dto.getMetadata().getMatchId());
        assertThat(saved.getParticipants()).hasSize(10);
        assertThat(lolMatchRepository.findByMatchId(saved.getMatchId())).isPresent();
    }

    @Test
    @DisplayName("계정 + 프로필 한 번에 저장")
    void saveAccountAndProfile() throws Exception {
        String accountJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_puuid_sample.json"));
        String profileJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_profile_sample.json"));
        LolAccountResponseDto accountDto = objectMapper.readValue(accountJson, LolAccountResponseDto.class);
        LolSummonerProfileDto profileDto = objectMapper.readValue(profileJson, LolSummonerProfileDto.class);

        var result = persistenceService.saveAccountAndProfile(accountDto, profileDto);

        assertThat(result.account()).isNotNull();
        assertThat(result.profile()).isNotNull();
        assertThat(result.account().getPuuid()).isEqualTo(result.profile().getPuuid());
    }

    @Test
    @DisplayName("매치 + 타임라인 JSON → DTO → Entity 저장")
    void saveMatchWithTimeline_jsonToDtoToEntity() throws Exception {
        String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
        String timelineJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_timeline_sample.json"));

        LolMatchDetailDto matchDto = lolMatchJsonService.parseMatchDetail(matchJson);
        LolMatchTimelineDetailDto timelineDto = lolMatchJsonService.parseTimelineDetail(timelineJson);

        LolMatch saved = persistenceService.saveMatchWithTimeline(matchDto, timelineDto);

        assertThat(saved).isNotNull();
        assertThat(saved.getMatchId()).isEqualTo("KR_8121193767");
        assertThat(saved.getParticipants()).hasSize(10);
        assertThat(saved.getTimeline()).isNotNull();
        assertThat(saved.getTimeline().getFrameInterval()).isEqualTo(60000);
        assertThat(saved.getTimeline().getTimelineInfo()).isNotBlank();
    }

    @Test
    @DisplayName("전체 파이프라인: 5종 JSON → DTO → Entity 검증")
    void fullPipeline_allSampleJsonToDtoToEntity() throws Exception {
        // 1. lol_puuid_sample.json → LolAccountResponseDto → LolAccount
        String puuidJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_puuid_sample.json"));
        LolAccountResponseDto accountDto = objectMapper.readValue(puuidJson, LolAccountResponseDto.class);
        LolAccount account = persistenceService.saveAccount(accountDto);
        assertThat(account).isNotNull();
        assertThat(account.getGameName()).isEqualTo("Hide on bush");

        // 2. lol_profile_sample.json → LolSummonerProfileDto → LolSummonerProfile
        String profileJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_profile_sample.json"));
        LolSummonerProfileDto profileDto = objectMapper.readValue(profileJson, LolSummonerProfileDto.class);
        LolSummonerProfile profile = persistenceService.saveSummonerProfile(profileDto);
        assertThat(profile).isNotNull();
        assertThat(profile.getSummonerLevel()).isEqualTo(888);

        // 3. lol_match_id_list_sample.json → LolMatchIdListResponseDto (엔티티 없음, 파싱만 검증)
        String matchIdListJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_id_list_sample.json"));
        LolMatchIdListResponseDto matchIdListDto = objectMapper.readValue(matchIdListJson, LolMatchIdListResponseDto.class);
        assertThat(matchIdListDto.getMatchIds()).containsExactly(
                "KR_8136533346", "KR_8136458444", "KR_8136370937", "KR_8136318964", "KR_8136054224");

        // 4. lol_match_sample.json → LolMatchDetailDto → LolMatch
        String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
        LolMatchDetailDto matchDto = lolMatchJsonService.parseMatchDetail(matchJson);
        LolMatch match = persistenceService.saveMatch(matchDto);
        assertThat(match).isNotNull();
        assertThat(match.getParticipants()).hasSize(10);
        assertThat(match.getTeams()).hasSize(2);

        // 5. lol_timeline_sample.json → LolMatchTimelineDetailDto → LolMatchTimeline
        String timelineJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_timeline_sample.json"));
        LolMatchTimelineDetailDto timelineDto = lolMatchJsonService.parseTimelineDetail(timelineJson);
        LolMatch matchWithTimeline = persistenceService.saveMatchWithTimeline(matchDto, timelineDto);
        assertThat(matchWithTimeline.getTimeline()).isNotNull();
        assertThat(matchWithTimeline.getTimeline().getFrameInterval()).isEqualTo(60000);
    }
}
