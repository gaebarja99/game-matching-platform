package com.gamematcher.dto.schedule;

import com.gamematcher.entity.BroadcastSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleItemDto {

    private Long id;
    private String title;
    private String scheduleAt;
    private String link;

    public static ScheduleItemDto from(BroadcastSchedule e) {
        if (e == null) return null;
        String at = e.getScheduleAt() == null ? null : e.getScheduleAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return ScheduleItemDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .scheduleAt(at)
                .link(e.getLink())
                .build();
    }
}
