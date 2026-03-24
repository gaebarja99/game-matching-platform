package com.gamematcher.service;

import com.gamematcher.entity.User;
import com.gamematcher.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfanityFilterService {

    private static final List<String> PROFANITY_KEYWORDS = List.of(
            "\uc2dc\ubc1c",
            "\uc528\ubc1c",
            "\uc314\ub144",
            "\ubcd1\uc2e0",
            "\ube44\uc2e0",
            "\uac1c\uc0c8\ub07c",
            "\uc874\ub098",
            "\uc870\ub098",
            "\uc9c0\ub784",
            "\uaebc\uc838",
            "\ubbf8\uce5c\ub144",
            "\ubbf8\uce5c\ub188",
            "fuck",
            "shit",
            "bitch",
            "asshole"
    );

    private final UserRepository userRepository;

    public boolean containsProfanity(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return PROFANITY_KEYWORDS.stream().map(this::normalize).anyMatch(normalized::contains);
    }

    public String maskProfanity(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String masked = text;
        for (String keyword : PROFANITY_KEYWORDS.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
            masked = masked.replaceAll("(?i)" + java.util.regex.Pattern.quote(keyword), "**");
        }
        if (!masked.equals(text)) {
            return masked;
        }
        return containsProfanity(text) ? "**" : text;
    }

    @Transactional
    public ModerationResult moderateChat(Long userId, String text) {
        ensureChatAllowed(userId);
        String sanitized = maskProfanity(text);
        boolean detected = containsProfanity(text);
        if (!detected || userId == null) {
            return new ModerationResult(sanitized, false, 0, null);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        int strikeCount = Optional.ofNullable(user.getProfanityStrikeCount()).orElse(0) + 1;
        user.setProfanityStrikeCount(strikeCount);

        LocalDateTime mutedUntil = user.getChatMutedUntil();
        LocalDateTime now = LocalDateTime.now();
        if (strikeCount % 10 == 0) {
            int penaltyMinutes = strikeCount / 10;
            LocalDateTime base = mutedUntil != null && mutedUntil.isAfter(now) ? mutedUntil : now;
            mutedUntil = base.plusMinutes(penaltyMinutes);
            user.setChatMutedUntil(mutedUntil);
        }

        userRepository.save(user);
        return new ModerationResult(sanitized, true, strikeCount, mutedUntil);
    }

    public void ensureChatAllowed(Long userId) {
        if (userId == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> {
            LocalDateTime mutedUntil = user.getChatMutedUntil();
            if (mutedUntil != null && mutedUntil.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException(buildMuteMessage(mutedUntil));
            }
        });
    }

    public String buildMuteMessage(LocalDateTime mutedUntil) {
        Duration remaining = Duration.between(LocalDateTime.now(), mutedUntil);
        long totalSeconds = Math.max(1L, remaining.getSeconds());
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes > 0) {
            return String.format(Locale.KOREAN, "채팅이 %d분 %d초 동안 금지되었습니다.", minutes, seconds);
        }
        return String.format(Locale.KOREAN, "채팅이 %d초 동안 금지되었습니다.", seconds);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9가-힣]", "");
    }

    @Getter
    @AllArgsConstructor
    public static class ModerationResult {
        private final String sanitizedText;
        private final boolean profanityDetected;
        private final int strikeCount;
        private final LocalDateTime mutedUntil;
    }
}
