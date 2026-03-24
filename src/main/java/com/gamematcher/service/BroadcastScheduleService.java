package com.gamematcher.service;

import com.gamematcher.dto.schedule.ScheduleItemDto;
import com.gamematcher.entity.BroadcastSchedule;
import com.gamematcher.repository.BroadcastScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BroadcastScheduleService {

    private final BroadcastScheduleRepository broadcastScheduleRepository;

    public List<ScheduleItemDto> list() {
        return broadcastScheduleRepository.findAllByOrderBySortOrderAscScheduleAtAsc().stream()
                .map(ScheduleItemDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleItemDto create(String title, LocalDateTime scheduleAt, String link) {
        BroadcastSchedule e = new BroadcastSchedule();
        e.setTitle(title != null ? title.trim() : "");
        e.setScheduleAt(scheduleAt);
        e.setLink(link != null && !link.isBlank() ? link.trim() : null);
        e.setSortOrder(0);
        e = broadcastScheduleRepository.save(e);
        return ScheduleItemDto.from(e);
    }

    @Transactional
    public ScheduleItemDto update(Long id, String title, LocalDateTime scheduleAt, String link) {
        BroadcastSchedule e = broadcastScheduleRepository.findById(id).orElse(null);
        if (e == null) return null;
        if (title != null) e.setTitle(title.trim());
        if (scheduleAt != null) e.setScheduleAt(scheduleAt);
        if (link != null) e.setLink(link.isBlank() ? null : link.trim());
        e = broadcastScheduleRepository.save(e);
        return ScheduleItemDto.from(e);
    }

    @Transactional
    public boolean delete(Long id) {
        if (!broadcastScheduleRepository.existsById(id)) return false;
        broadcastScheduleRepository.deleteById(id);
        return true;
    }
}
