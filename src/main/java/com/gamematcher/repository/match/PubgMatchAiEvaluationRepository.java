package com.gamematcher.repository.match;

import com.gamematcher.entity.match.pubg.PubgMatchAiEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PubgMatchAiEvaluationRepository extends JpaRepository<PubgMatchAiEvaluation, Long> {

    Optional<PubgMatchAiEvaluation> findByPubgMatchParticipant_IdAndLlmModel(Long pubgMatchParticipantId, String llmModel);

    List<PubgMatchAiEvaluation> findByPubgMatchParticipant_Match_MatchId(String matchId);

    @Query("select e from PubgMatchAiEvaluation e join e.pubgMatchParticipant p join p.match m "
            + "where m.matchId = :matchId and p.playerId = :accountId and e.llmModel = :llmModel")
    Optional<PubgMatchAiEvaluation> findByMatchIdAndAccountIdAndLlmModel(
            @Param("matchId") String matchId,
            @Param("accountId") String accountId,
            @Param("llmModel") String llmModel);

    /** 구 스키마(모델 컬럼 없음) 마이그레이션 직후 null/빈 문자열 행 */
    @Query("select e from PubgMatchAiEvaluation e join e.pubgMatchParticipant p join p.match m "
            + "where m.matchId = :matchId and p.playerId = :accountId "
            + "and (e.llmModel is null or e.llmModel = '')")
    Optional<PubgMatchAiEvaluation> findByMatchIdAndAccountIdLegacyBlankModel(
            @Param("matchId") String matchId,
            @Param("accountId") String accountId);
}
