package com.gamematcher.dto.report;

import com.gamematcher.entity.BlockedUser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class BlockedUserResponseDto {

    private Long id;
    private Long blockedUserId;
    private String blockedUsername;
    private LocalDateTime blockedAt;

    public static BlockedUserResponseDto from(BlockedUser blockedUser) {
        BlockedUserResponseDto dto = new BlockedUserResponseDto();
        dto.setId(blockedUser.getId());
        dto.setBlockedUserId(blockedUser.getBlockedUser().getId());
        dto.setBlockedUsername(blockedUser.getBlockedUser().getUsername());
        dto.setBlockedAt(blockedUser.getCreatedAt());
        return dto;
    }
}
