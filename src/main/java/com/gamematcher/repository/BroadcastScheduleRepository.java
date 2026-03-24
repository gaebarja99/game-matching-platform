package com.gamematcher.repository;

import com.gamematcher.entity.BroadcastSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BroadcastScheduleRepository extends JpaRepository<BroadcastSchedule, Long> {

    List<BroadcastSchedule> findAllByOrderBySortOrderAscScheduleAtAsc();
}
