package com.gamematcher.repository;

import com.gamematcher.entity.MatchRecordParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRecordParticipantRepository extends JpaRepository<MatchRecordParticipant, Long> {

    List<MatchRecordParticipant> findByMatchRecordId(Long matchRecordId);

    List<MatchRecordParticipant> findByUserId(Long userId);
}
