package com.gamematcher.config;

import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.lol.LolMatchTimelineDetailDto;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import com.gamematcher.mapper.ValorantMatchMapper;
import com.gamematcher.repository.match.ValorantMatchRepository;
import com.gamematcher.service.lol.LolMatchJsonService;
import com.gamematcher.service.lol.LolMatchJsonService.LolMatchJsonParseException;
import com.gamematcher.service.lol.LolMatchService;
import com.gamematcher.service.valorant.ValorantMatchJsonService;
import com.gamematcher.service.valorant.ValorantMatchJsonService.ValorantMatchJsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 샘플 JSON을 JsonService + Mapper 경유로 DB에 삽입.
 * Profile "sample" 활성화 시 기동 후 실행.
 *
 * 예: mvn spring-boot:run -Dspring-boot.run.profiles=sample
 */
@Component
@Profile("sample")
public class SampleDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    /** 샘플 파일 경로 (기본: 프로젝트 루트 기준 src/test/resources/samples) */
    private static final String DEFAULT_SAMPLE_PATH = "src/test/resources/samples";

    /** DB에 넣을 Valorant 매치 수 제한 (0 = 제한 없음) */
    private static final int DEFAULT_VALORANT_MATCH_LIMIT = 5;

    private final ValorantMatchJsonService valorantMatchJsonService;
    private final ValorantMatchMapper valorantMatchMapper;
    private final ValorantMatchRepository valorantMatchRepository;
    private final LolMatchJsonService lolMatchJsonService;
    private final LolMatchService lolMatchService;
    private final Environment env;

    public SampleDataLoader(ValorantMatchJsonService valorantMatchJsonService,
                            ValorantMatchMapper valorantMatchMapper,
                            ValorantMatchRepository valorantMatchRepository,
                            LolMatchJsonService lolMatchJsonService,
                            LolMatchService lolMatchService,
                            Environment env) {
        this.valorantMatchJsonService = valorantMatchJsonService;
        this.valorantMatchMapper = valorantMatchMapper;
        this.valorantMatchRepository = valorantMatchRepository;
        this.lolMatchJsonService = lolMatchJsonService;
        this.lolMatchService = lolMatchService;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        String basePath = env.getProperty("sample.data.path", DEFAULT_SAMPLE_PATH);
        int valorantLimit = env.getProperty("sample.valorant.match-limit", Integer.class, DEFAULT_VALORANT_MATCH_LIMIT);

        Path sampleDir = Path.of(System.getProperty("user.dir", "."), basePath);
        if (!Files.isDirectory(sampleDir)) {
            log.warn("[SampleDataLoader] 샘플 디렉토리 없음: {}", sampleDir);
            return;
        }

        loadValorantSamples(sampleDir.resolve("valorant"), valorantLimit);
        loadLolSamples(sampleDir.resolve("lol"));
    }

    private void loadValorantSamples(Path valorantDir, int limit) {
        Path file = valorantDir.resolve("valorant_match_sample.json");
        if (!Files.isRegularFile(file)) {
            log.info("[SampleDataLoader] Valorant 샘플 파일 없음: {}", file);
            return;
        }

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            List<ValorantMatchDetailDto> matches = valorantMatchJsonService.parseMatchesFromApiResponse(json);

            if (matches.isEmpty()) {
                log.info("[SampleDataLoader] Valorant 샘플에 매치가 없습니다.");
                return;
            }

            int saved = 0;
            int max = limit > 0 ? Math.min(limit, matches.size()) : matches.size();

            for (int i = 0; i < max; i++) {
                ValorantMatchDetailDto dto = matches.get(i);
                if (dto.getMetadata() == null || dto.getMetadata().getMatchId() == null) {
                    continue;
                }
                String matchId = dto.getMetadata().getMatchId();
                if (valorantMatchRepository.existsByMatchId(matchId)) {
                    log.debug("[SampleDataLoader] 이미 존재하는 매치 건너뜀: {}", matchId);
                    continue;
                }

                ValorantMatch entity = valorantMatchMapper.toEntity(dto);
                if (entity != null) {
                    valorantMatchRepository.save(entity);
                    saved++;
                    log.info("[SampleDataLoader] Valorant 매치 저장: {}", matchId);
                }
            }

            log.info("[SampleDataLoader] Valorant 샘플 {} 건 저장 완료", saved);

        } catch (ValorantMatchJsonParseException e) {
            log.error("[SampleDataLoader] Valorant JSON 파싱 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[SampleDataLoader] Valorant 샘플 로드 실패", e);
        }
    }

    private void loadLolSamples(Path lolDir) {
        Path matchFile = lolDir.resolve("lol_match_sample.json");
        Path timelineFile = lolDir.resolve("lol_timeline_sample.json");

        if (!Files.isRegularFile(matchFile)) {
            log.info("[SampleDataLoader] LoL 매치 샘플 파일 없음: {}", matchFile);
            return;
        }

        try {
            String matchJson = Files.readString(matchFile, StandardCharsets.UTF_8);
            LolMatchDetailDto matchDto = lolMatchJsonService.parseFirstMatch(matchJson);

            if (matchDto == null || matchDto.getMetadata() == null || matchDto.getMetadata().getMatchId() == null) {
                log.info("[SampleDataLoader] LoL 샘플 파싱 실패");
                return;
            }

            LolMatchTimelineDetailDto timelineDto = null;
            if (Files.isRegularFile(timelineFile)) {
                String timelineJson = Files.readString(timelineFile, StandardCharsets.UTF_8);
                timelineDto = lolMatchJsonService.parseFirstTimeline(timelineJson);
            }

            var saved = lolMatchService.saveMatchWithTimeline(matchDto, timelineDto);
            if (saved != null) {
                log.info("[SampleDataLoader] LoL 매치 저장: {}", matchDto.getMetadata().getMatchId());
            } else {
                log.debug("[SampleDataLoader] LoL 매치 이미 존재: {}", matchDto.getMetadata().getMatchId());
            }

        } catch (LolMatchJsonParseException e) {
            log.error("[SampleDataLoader] LoL JSON 파싱 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[SampleDataLoader] LoL 샘플 로드 실패", e);
        }
    }
}
