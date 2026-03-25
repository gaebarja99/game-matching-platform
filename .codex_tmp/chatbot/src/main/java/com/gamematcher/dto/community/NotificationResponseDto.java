package com.gamematcher.dto.community;

import com.gamematcher.constant.community.NotificationType;
import com.gamematcher.entity.community.Notification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NotificationResponseDto {

    private Long id;
    private NotificationType type;
    private String message;
    private Long postId;
    private Long commentId;
    private Long actorId;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.id = notification.getId();
        dto.type = notification.getType();
        dto.message = notification.getMessage();
        dto.postId = notification.getPostId();
        dto.commentId = notification.getCommentId();
        dto.actorId = notification.getActorId();
        dto.isRead = notification.isRead();
        dto.createdAt = notification.getCreatedAt();
        return dto;
    }
}
