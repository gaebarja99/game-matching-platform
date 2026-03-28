package com.gamematcher.service;

import com.gamematcher.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ProfanityFilterServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ProfanityFilterService profanityFilterService;

    @Test
    @DisplayName("Detects Korean profanity variants")
    void containsProfanity_detectsKoreanVariants() {
        assertThat(profanityFilterService.containsProfanity("\uC870\uAE4C")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uC88B\uAE4C")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uC528\uBC1C\uB828")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uC2DC\uBC14\uAEBC")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uBBF8\uCE5C\uC0C8\uB07C")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uC560\uBBF8")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uB4A4\uC9C4")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uC560\uBBF8\uB4A4\uC84C\uB0D0?")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uB290\uAE08\uB9C8")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uB290\uADF8\uBE60")).isTrue();
    }

    @Test
    @DisplayName("Detects profanity across spaces and punctuation")
    void containsProfanity_detectsNormalizedProfanity() {
        assertThat(profanityFilterService.containsProfanity("\uC2DC \uBC1C")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uC2DC \uBC14 \uAEBC")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uC560\uBBF8~\uB4A4\uC84C\uB0D0")).isTrue();
        assertThat(profanityFilterService.containsProfanity("\uB290 \uAE08 \uB9C8")).isTrue();
    }

    @Test
    @DisplayName("Masks compound variants")
    void maskProfanity_masksVariants() {
        assertThat(profanityFilterService.maskProfanity("\uBBF8\uCE5C\uC0C8\uB07C")).isEqualTo("**");
        assertThat(profanityFilterService.maskProfanity("\uC2DC\uBC14\uAEBC")).isEqualTo("**");
        assertThat(profanityFilterService.maskProfanity("\uC560\uBBF8\uB4A4\uC84C\uB0D0?")).isEqualTo("**");
        assertThat(profanityFilterService.maskProfanity("\uB290\uAE08\uB9C8")).isEqualTo("**");
    }

    @Test
    @DisplayName("Allows normal sentences")
    void containsProfanity_allowsNormalSentence() {
        assertThat(profanityFilterService.containsProfanity("\uC548\uB155\uD558\uC138\uC694 \uBC18\uAC11\uC2B5\uB2C8\uB2E4")).isFalse();
        assertThat(profanityFilterService.maskProfanity("\uC624\uB298 \uB9E4\uCE6D \uAC19\uC774 \uD558\uC2E4\uB798\uC694?"))
                .isEqualTo("\uC624\uB298 \uB9E4\uCE6D \uAC19\uC774 \uD558\uC2E4\uB798\uC694?");
    }
}
