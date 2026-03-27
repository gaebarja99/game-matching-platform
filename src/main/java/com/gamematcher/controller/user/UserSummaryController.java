package com.gamematcher.controller.user;

import com.gamematcher.dto.user.UserSummaryResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.MatchSessionMemberRepository;
import com.gamematcher.repository.UserRepository;
import com.gamematcher.service.LevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSummaryController {

    private final UserRepository userRepository;
    private final MatchSessionMemberRepository matchSessionMemberRepository;

    @GetMapping("/{userId}/summary")
    public UserSummaryResponseDto getUserSummary(@PathVariable Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        int level = LevelService.getLevel(u.getTotalExperienceTenths() != null ? u.getTotalExperienceTenths() : 0L);
        long matchCount = matchSessionMemberRepository.countByUserId(userId);

        return new UserSummaryResponseDto(
                u.getId(),
                u.getUsername(),
                u.getNickname(),
                u.getProfileImageUrl(),
                u.getCreatedAt(),
                level,
                matchCount
        );
    }
}

