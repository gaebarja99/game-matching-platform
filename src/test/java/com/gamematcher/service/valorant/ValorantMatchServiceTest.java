package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.mapper.ValorantMatchMapper;
import com.gamematcher.repository.match.ValorantMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValorantMatchService 테스트")
class ValorantMatchServiceTest {

    @Mock
    private ValorantMatchRepository valorantMatchRepository;

    @Mock
    private ValorantMatchMapper valorantMatchMapper;

    @InjectMocks
    private ValorantMatchService valorantMatchService;

    private ValorantMatchDetailDto validDto;
    private ValorantMatch savedEntity;

    @BeforeEach
    void setUp() throws Exception {
        String jsonPath = "src/test/resources/samples/valorant/valorant_match_sample.json";
        String json = Files.readString(Paths.get(jsonPath));
        validDto = new ValorantMatchJsonService().parseFirstMatch(json);

        savedEntity = new ValorantMatch();
        savedEntity.setId(1L);
        savedEntity.setMatchId(validDto.getMetadata().getMatchId());
    }

    @Nested
    @DisplayName("saveMatch - 단일 매치 저장")
    class SaveMatchTest {

        @Test
        @DisplayName("null DTO가 주어지면 null을 반환하고 저장하지 않는다")
        void saveMatch_nullDto_returnsNullAndDoesNotSave() {
            ValorantMatch result = valorantMatchService.saveMatch(null);

            assertThat(result).isNull();
            verify(valorantMatchRepository, never()).existsByMatchId(any());
            verify(valorantMatchMapper, never()).toEntity(any());
            verify(valorantMatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("metadata가 null이면 null을 반환하고 저장하지 않는다")
        void saveMatch_metadataNull_returnsNullAndDoesNotSave() {
            ValorantMatchDetailDto dto = new ValorantMatchDetailDto();
            dto.setMetadata(null);

            ValorantMatch result = valorantMatchService.saveMatch(dto);

            assertThat(result).isNull();
            verify(valorantMatchRepository, never()).existsByMatchId(any());
            verify(valorantMatchMapper, never()).toEntity(any());
            verify(valorantMatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("matchId가 null이면 null을 반환하고 저장하지 않는다")
        void saveMatch_matchIdNull_returnsNullAndDoesNotSave() {
            ValorantMatchDetailDto dto = new ValorantMatchDetailDto();
            var meta = new com.gamematcher.dto.valorant.ValorantMatchInfoDto();
            meta.setMatchId(null);
            dto.setMetadata(meta);

            ValorantMatch result = valorantMatchService.saveMatch(dto);

            assertThat(result).isNull();
            verify(valorantMatchRepository, never()).existsByMatchId(any());
            verify(valorantMatchMapper, never()).toEntity(any());
            verify(valorantMatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 존재하는 matchId면 null을 반환하고 저장하지 않는다")
        void saveMatch_duplicateMatchId_returnsNullAndDoesNotSave() {
            String matchId = validDto.getMetadata().getMatchId();
            when(valorantMatchRepository.existsByMatchId(matchId)).thenReturn(true);

            ValorantMatch result = valorantMatchService.saveMatch(validDto);

            assertThat(result).isNull();
            verify(valorantMatchRepository).existsByMatchId(matchId);
            verify(valorantMatchMapper, never()).toEntity(any());
            verify(valorantMatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("유효한 DTO가 주어지면 저장하고 엔티티를 반환한다")
        void saveMatch_validDto_savesAndReturnsEntity() {
            String matchId = validDto.getMetadata().getMatchId();
            when(valorantMatchRepository.existsByMatchId(matchId)).thenReturn(false);
            when(valorantMatchMapper.toEntity(validDto)).thenReturn(savedEntity);
            when(valorantMatchRepository.save(any(ValorantMatch.class))).thenReturn(savedEntity);

            ValorantMatch result = valorantMatchService.saveMatch(validDto);

            assertThat(result).isNotNull();
            assertThat(result.getMatchId()).isEqualTo(matchId);
            verify(valorantMatchRepository).existsByMatchId(matchId);
            verify(valorantMatchMapper).toEntity(validDto);
            verify(valorantMatchRepository).save(any(ValorantMatch.class));
        }
    }

    @Nested
    @DisplayName("saveMatches - 매치 목록 저장")
    class SaveMatchesTest {

        @Test
        @DisplayName("null 목록이 주어지면 0을 반환한다")
        void saveMatches_nullList_returnsZero() {
            int result = valorantMatchService.saveMatches(null, 5);

            assertThat(result).isZero();
            verify(valorantMatchRepository, never()).existsByMatchId(any());
            verify(valorantMatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("빈 목록이 주어지면 0을 반환한다")
        void saveMatches_emptyList_returnsZero() {
            int result = valorantMatchService.saveMatches(Collections.emptyList(), 5);

            assertThat(result).isZero();
            verify(valorantMatchRepository, never()).existsByMatchId(any());
            verify(valorantMatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("limit가 0이면 전체 목록을 저장한다")
        void saveMatches_limitZero_savesAll() {
            List<ValorantMatchDetailDto> dtos = List.of(validDto);
            when(valorantMatchRepository.existsByMatchId(validDto.getMetadata().getMatchId())).thenReturn(false);
            when(valorantMatchMapper.toEntity(validDto)).thenReturn(savedEntity);
            when(valorantMatchRepository.save(any(ValorantMatch.class))).thenReturn(savedEntity);

            int result = valorantMatchService.saveMatches(dtos, 0);

            assertThat(result).isEqualTo(1);
            verify(valorantMatchRepository).save(any(ValorantMatch.class));
        }

        @Test
        @DisplayName("limit가 설정되면 해당 개수만 저장한다")
        void saveMatches_withLimit_savesUpToLimit() {
            ValorantMatchDetailDto dto2 = new ValorantMatchDetailDto();
            var meta2 = new com.gamematcher.dto.valorant.ValorantMatchInfoDto();
            meta2.setMatchId("match-id-2");
            dto2.setMetadata(meta2);

            List<ValorantMatchDetailDto> dtos = List.of(validDto, dto2);
            when(valorantMatchRepository.existsByMatchId(any())).thenReturn(false);
            when(valorantMatchMapper.toEntity(any())).thenReturn(savedEntity);
            when(valorantMatchRepository.save(any(ValorantMatch.class))).thenReturn(savedEntity);

            int result = valorantMatchService.saveMatches(dtos, 1);

            assertThat(result).isEqualTo(1);
            verify(valorantMatchRepository, org.mockito.Mockito.times(1)).save(any(ValorantMatch.class));
        }

        @Test
        @DisplayName("중복된 매치는 건너뛰고 새 매치만 저장한다")
        void saveMatches_duplicates_skipsAndSavesOnlyNew() {
            ValorantMatchDetailDto dto2 = new ValorantMatchDetailDto();
            var meta2 = new com.gamematcher.dto.valorant.ValorantMatchInfoDto();
            meta2.setMatchId("match-id-2");
            dto2.setMetadata(meta2);

            List<ValorantMatchDetailDto> dtos = List.of(validDto, dto2);

            when(valorantMatchRepository.existsByMatchId(validDto.getMetadata().getMatchId())).thenReturn(true);
            when(valorantMatchRepository.existsByMatchId("match-id-2")).thenReturn(false);
            when(valorantMatchMapper.toEntity(dto2)).thenReturn(savedEntity);
            when(valorantMatchRepository.save(any(ValorantMatch.class))).thenReturn(savedEntity);

            int result = valorantMatchService.saveMatches(dtos, 5);

            assertThat(result).isEqualTo(1);
            verify(valorantMatchMapper, never()).toEntity(validDto);
            verify(valorantMatchMapper).toEntity(dto2);
        }
    }
}
