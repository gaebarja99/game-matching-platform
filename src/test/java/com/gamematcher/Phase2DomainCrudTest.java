package com.gamematcher;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.constant.ai.evaluation.EvaluationStatus;
import com.gamematcher.constant.ai.evaluation.Grade;
import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.BaseStatsDTO;
import com.gamematcher.dto.ai.evaluation.GenericStatsDTO;
import com.gamematcher.dto.ai.evaluation.LolStatsDTO;
import com.gamematcher.dto.ai.evaluation.StatsConverter;
import com.gamematcher.dto.ai.evaluation.ValorantMatchStatsDTO;
import com.gamematcher.entity.User;
import com.gamematcher.entity.ai.evaluation.Game;
import com.gamematcher.entity.ai.evaluation.MatchRecord;
import com.gamematcher.entity.ai.evaluation.MatchRecordEvaluation;
import com.gamematcher.entity.ai.evaluation.MatchRecordParticipant;
import com.gamematcher.repository.ai.evaluation.GameRepository;
import com.gamematcher.repository.ai.evaluation.MatchRecordEvaluationRepository;
import com.gamematcher.repository.ai.evaluation.MatchRecordParticipantRepository;
import com.gamematcher.repository.ai.evaluation.MatchRecordRepository;
import com.gamematcher.repository.common.CommonUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2: 도메인 및 저장소 CRUD 검증.
 * 더미 게임/매치 데이터로 생성·조회·StatsConverter 변환 검증.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase2DomainCrudTest {

    @Autowired GameRepository gameRepository;
    @Autowired MatchRecordRepository matchRecordRepository;
    @Autowired MatchRecordParticipantRepository participantRepository;
    @Autowired MatchRecordEvaluationRepository evaluationRepository;
    @Autowired CommonUserRepository userRepository;
    @Autowired StatsConverter statsConverter;

    @Test
    @DisplayName("Game CRUD - 게임 생성 및 조회")
    void gameCrud() {
        Game valorant = new Game("발로란트", "VALORANT", "{}");
        Game saved = gameRepository.save(valorant);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode()).isEqualTo("VALORANT");
        assertThat(gameRepository.findByCode("VALORANT")).contains(saved);
    }

    @Test
    @DisplayName("MatchRecord CRUD - 매치 기록 생성 및 조회")
    void matchRecordCrud() {
        Game game = gameRepository.save(new Game("롤", "LEAGUE_OF_LEGENDS", null));
        MatchRecord record = new MatchRecord(
            game, "match-001", MatchResult.VICTORY,
            LocalDateTime.now(),
            Map.of("duration", 25, "mode", "ranked")
        );
        MatchRecord saved = matchRecordRepository.save(record);

        assertThat(saved.getId()).isNotNull();
        assertThat(matchRecordRepository.findByMatchId("match-001")).contains(saved);
    }

    @Test
    @DisplayName("MatchRecordParticipant - rawStats 저장 및 StatsConverter 변환")
    void participantWithRawStatsAndConverter() {
        User user = createTestUser();
        Game game = gameRepository.save(new Game("발로란트", "VALORANT", null));
        MatchRecord record = matchRecordRepository.save(
            new MatchRecord(game, "match-v1", MatchResult.VICTORY, LocalDateTime.now(), null)
        );

        ValorantMatchStatsDTO stats = ValorantMatchStatsDTO.builder()
            .game("VALORANT")
            .kills(20)
            .deaths(5)
            .assists(10)
            .result(MatchResult.VICTORY)
            .roundsWon(13)
            .roundsPlayed(24)
            .headShots(8)
            .totalShots(40)
            .build();
        String rawStatsJson = statsConverter.toJson(stats);

        MatchRecordParticipant participant = new MatchRecordParticipant(record, user, "Duelist", rawStatsJson);
        MatchRecordParticipant saved = participantRepository.save(participant);

        assertThat(saved.getRawStats()).isNotNull();
        BaseStatsDTO dto = statsConverter.toDto(saved.getRawStats(), game.getCode());
        assertThat(dto).isInstanceOf(ValorantMatchStatsDTO.class);
        assertThat(dto.getKills()).isEqualTo(20);
        assertThat(((ValorantMatchStatsDTO) dto).getRoundsWon()).isEqualTo(13);
    }

    @Test
    @DisplayName("LolStatsDTO - JSON 직렬화/역직렬화")
    void lolStatsConverterRoundTrip() {
        LolStatsDTO original = new LolStatsDTO(
            "LEAGUE_OF_LEGENDS", 8, 2, 15, MatchResult.VICTORY,
            12500, 180, 25000L, 45, 32
        );
        String json = statsConverter.toJson(original);
        BaseStatsDTO parsed = statsConverter.toDto(json, "LEAGUE_OF_LEGENDS");

        assertThat(parsed).isInstanceOf(LolStatsDTO.class);
        LolStatsDTO parsedLol = (LolStatsDTO) parsed;
        assertThat(parsedLol.getGold()).isEqualTo(12500);
        assertThat(parsedLol.getMinionsKilled()).isEqualTo(180);
    }

    @Test
    @DisplayName("MatchRecordEvaluation CRUD - 평가 생성 및 참가자별 조회")
    void evaluationCrud() {
        User user = createTestUser();
        Game game = gameRepository.save(new Game("발로란트", "VALORANT", null));
        MatchRecord record = matchRecordRepository.save(
            new MatchRecord(game, "match-eval", MatchResult.VICTORY, LocalDateTime.now(), null)
        );
        MatchRecordParticipant participant = participantRepository.save(
            new MatchRecordParticipant(record, user, "Controller", "{}")
        );

        MatchRecordEvaluation eval = new MatchRecordEvaluation(
            participant, EvaluationStatus.COMPLETED,
            150, "훌륭한 결과", "상세 코멘트..."
        );
        MatchRecordEvaluation saved = evaluationRepository.save(eval);

        assertThat(saved.getScore()).isEqualTo(150);
        assertThat(saved.getGrade()).isEqualTo(Grade.S);
        List<MatchRecordEvaluation> byMatch = evaluationRepository.findByParticipant_MatchRecordId(record.getId());
        assertThat(byMatch).hasSize(1).contains(saved);
    }

    @Test
    @DisplayName("DB 중심 - 전용 DTO 없는 새 게임 코드는 GenericStatsDTO로 변환")
    void newGameCodeUsesGenericStatsDto() {
        Game newGame = gameRepository.save(new Game("신작게임", "NEW_GAME_2025", null));
        MatchRecord record = matchRecordRepository.save(
            new MatchRecord(newGame, "match-new", MatchResult.VICTORY, LocalDateTime.now(), null)
        );
        User user = createTestUser();

        GenericStatsDTO stats = new GenericStatsDTO("NEW_GAME_2025", 5, 3, 2, MatchResult.VICTORY);
        String rawStatsJson = statsConverter.toJson(stats);
        MatchRecordParticipant participant = participantRepository.save(
            new MatchRecordParticipant(record, user, "Attacker", rawStatsJson)
        );

        BaseStatsDTO dto = statsConverter.toDto(participant.getRawStats(), newGame.getCode());
        assertThat(dto).isInstanceOf(GenericStatsDTO.class);
        assertThat(dto.getGame()).isEqualTo("NEW_GAME_2025");
        assertThat(dto.getKills()).isEqualTo(5);
    }

    @Test
    @DisplayName("유저별 최근 평가 목록 페이지네이션")
    void userEvaluationsPagination() {
        User user = createTestUser();
        Game game = gameRepository.save(new Game("롤", "LEAGUE_OF_LEGENDS", null));
        MatchRecord record = matchRecordRepository.save(
            new MatchRecord(game, "match-paged", MatchResult.VICTORY, LocalDateTime.now(), null)
        );
        MatchRecordParticipant participant = participantRepository.save(
            new MatchRecordParticipant(record, user, "Mid", "{}")
        );
        evaluationRepository.save(new MatchRecordEvaluation(
            participant, EvaluationStatus.COMPLETED, 110, "요약", "상세"
        ));

        var page = evaluationRepository.findByParticipant_UserIdOrderByEvaluatedAtDesc(
            user.getId(), PageRequest.of(0, 10)
        );
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).getParticipant().getUser().getId()).isEqualTo(user.getId());
    }

    private User createTestUser() {
        User user = new User();
        user.setLoginId("test-user-" + System.currentTimeMillis());
        user.setUsername("테스트유저");
        user.setEmail("test@test.com");
        user.setPassword("encoded");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}
