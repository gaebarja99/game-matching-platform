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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProfanityFilterService {

    private static final String HANGUL_RANGE = "\uAC00-\uD7A3";

    private static final List<String> PROFANITY_KEYWORDS = List.of(
            "\uC2DC\uBC1C",
            "\uC528\uBC1C",
            "\uC2DC\uBC14",
            "\uC2DC\uBC14\uAEBC",
            "\uC2DC\uBC14\uB188",
            "\uC2DC\uBC1C\uB828",
            "\uC528\uBC1C\uB828",
            "\uBCD1\uC2E0",
            "\uBE05\uC2E0",
            "\uBE59\uC2E0",
            "\uAC1C\uC0C8\uB07C",
            "\uC0C8\uB07C",
            "\uC50D\uC0C8\uB07C",
            "\uC874\uB098",
            "\uC878\uB77C",
            "\uC88B",
            "\uC883",
            "\uC88B\uAE4C",
            "\uC870\uAE4C",
            "\uC9C0\uB784",
            "\uAEBC\uC838",
            "\uC5FC\uBCD1",
            "\uBBF8\uCE5C\uB144",
            "\uBBF8\uCE5C\uB188",
            "\uBBF8\uCE5C\uC0C8\uB07C",
            "\uC560\uBBF8",
            "\uB2C8\uC560\uBBF8",
            "\uB290\uAE08\uB9C8",
            "\uB290\uADF8\uB9C8",
            "\uB290\uADF8\uC5C4\uB9C8",
            "\uB290\uADF8\uBE60",
            "\uB4A4\uC838",
            "\uB4A4\uC84C",
            "\uB4A4\uC9C4",
            "fuck",
            "shit",
            "bitch",
            "asshole"
    );

    private static final List<Pattern> PROFANITY_PATTERNS = List.of(
            Pattern.compile("\uC2DC+\uBC1C+"),
            Pattern.compile("\uC528+\uBC1C+"),
            Pattern.compile("\uC2DC+\uBC14+"),
            Pattern.compile("\uC2DC+\uBC14+\uAEDC*"),
            Pattern.compile("\uC2DC+\uBC14+\uB188*"),
            Pattern.compile("\uC50D+"),
            Pattern.compile("[\uBCD1\uBE05\uBE59]\uC2E0"),
            Pattern.compile("\uAC1C?\uC0C8+\uB07C"),
            Pattern.compile("\uC0C8+\uB07C"),
            Pattern.compile("[\uC874\uC878]\uB098"),
            Pattern.compile("[\uC88B\uC883\uC870]\uAE4C"),
            Pattern.compile("[\uC88B\uC883]"),
            Pattern.compile("\uC9C0+\uB784+"),
            Pattern.compile("\uAEBC+\uC838"),
            Pattern.compile("\uC5FC\uBCD1"),
            Pattern.compile("\uBBF8\uCE5C(\uB144|\uB188|\uC0C8\uB07C|\uB828)"),
            Pattern.compile("(\uB2C8|\uB290\uADF8|\uB290\uAE08|\uC560)\uBBF8"),
            Pattern.compile("\uB290\uADF8\uBE60"),
            Pattern.compile("\uC560\uBBF8\uB4A4\uC84C"),
            Pattern.compile("\uB4A4\uC838"),
            Pattern.compile("\uB4A4\uC84C"),
            Pattern.compile("\uB4A4\uC9C4"),
            Pattern.compile("fuck"),
            Pattern.compile("shit"),
            Pattern.compile("bitch"),
            Pattern.compile("asshole")
    );

    private final UserRepository userRepository;

    public boolean containsProfanity(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return PROFANITY_KEYWORDS.stream()
                .map(this::normalize)
                .anyMatch(normalized::contains)
                || PROFANITY_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(normalized).find());
    }

    public String maskProfanity(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String masked = text;
        for (String keyword : PROFANITY_KEYWORDS.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
            masked = masked.replaceAll("(?i)" + Pattern.quote(keyword), "**");
        }
        if (!masked.equals(text) && containsProfanity(masked)) {
            return "**";
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
                .orElseThrow(() -> new IllegalArgumentException("\uC0AC\uC6A9\uC790\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
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
            return String.format(Locale.KOREAN, "\uCC44\uD305\uC774 %d\uBD84 %d\uCD08 \uB3D9\uC548 \uAE08\uC9C0\uB418\uC5C8\uC2B5\uB2C8\uB2E4.", minutes, seconds);
        }
        return String.format(Locale.KOREAN, "\uCC44\uD305\uC774 %d\uCD08 \uB3D9\uC548 \uAE08\uC9C0\uB418\uC5C8\uC2B5\uB2C8\uB2E4.", seconds);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9" + HANGUL_RANGE + "]", "");
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
