package com.gamematcher.repository;

import com.gamematcher.entity.MatchRecordEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRecordEvaluationRepository extends JpaRepository<MatchRecordEvaluation, Long> {

    Optional<MatchRecordEvaluation> findTop1ByParticipant_IdOrderByEvaluatedAtDesc(Long participantId);

    List<MatchRecordEvaluation> findByParticipant_MatchRecordId(Long matchRecordId);

    Page<MatchRecordEvaluation> findByParticipant_UserIdOrderByEvaluatedAtDesc(Long userId, Pageable pageable);

    boolean existsByParticipantId(Long participantId);
}
