package com.gamematcher.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** 라이브 채팅 메시지 (브로드캐스트용) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long userId;
    /** 로그인 시 사용하는 아이디 (표시용) */
    private String loginId;
    private String displayName;
    /** 프로필 이미지 URL (채팅 닉네임 옆 표시용) */
    private String profileImageUrl;
    private String text;
    /** true면 해당 방송의 스트리머(방송자) */
    private Boolean streamer;
    private Boolean manager;
    /** 채팅창 뱃지용 레벨 (1~9999). null이면 미표시 */
    private Integer level;
    @Builder.Default
    private long timestamp = Instant.now().toEpochMilli();
}
