package com.gamematcher.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.lol.LolMatchTimelineDetailDto;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.dto.valorant.ValorantMmrHistoryApiResponse;
import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import com.gamematcher.service.lol.LolMatchJsonService;
import com.gamematcher.service.lol.LolMatchJsonService.LolMatchJsonParseException;
import com.gamematcher.service.lol.LolMatchService;
import com.gamematcher.service.valorant.*;
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

    private final ValorantMatchJsonService valorantMatchJsonService;
    private final ValorantMatchService valorantMatchService;
    private final ValorantAccountService valorantAccountService;
    private final ValorantLifetimeJsonService valorantLifetimeJsonService;
    private final ValorantLifetimeService valorantLifetimeService;
    private final ValorantMmrService valorantMmrService;
    private final ValorantMmrHistoryService valorantMmrHistoryService;
    private final LolMatchJsonService lolMatchJsonService;
    private final LolMatchService lolMatchService;
    private final Environment env;

    private final ObjectMapper objectMapper = new ObjectMapper() {{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }};

    public SampleDataLoader(ValorantMatchJsonService valorantMatchJsonService,
                            ValorantMatchService valorantMatchService,
                            ValorantAccountService valorantAccountService,
                            ValorantLifetimeJsonService valorantLifetimeJsonService,
                            ValorantLifetimeService valorantLifetimeService,
                            ValorantMmrService valorantMmrService,
                            ValorantMmrHistoryService valorantMmrHistoryService,
                            LolMatchJsonService lolMatchJsonService,
                            LolMatchService lolMatchService,
                            Environment env) {
        this.valorantMatchJsonService = valorantMatchJsonService;
        this.valorantMatchService = valorantMatchService;
        this.valorantAccountService = valorantAccountService;
        this.valorantLifetimeJsonService = valorantLifetimeJsonService;
        this.valorantLifetimeService = valorantLifetimeService;
        this.valorantMmrService = valorantMmrService;
        this.valorantMmrHistoryService = valorantMmrHistoryService;
        this.lolMatchJsonService = lolMatchJsonService;
        this.lolMatchService = lolMatchService;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        String basePath = env.getProperty("sample.data.path", DEFAULT_SAMPLE_PATH);

        Path sampleDir = Path.of(System.getProperty("user.dir", "."), basePath);
        if (!Files.isDirectory(sampleDir)) {
            log.warn("[SampleDataLoader] 샘플 디렉토리 없음: {}", sampleDir);
            return;
        }

        loadValorantSamples(sampleDir.resolve("valorant"));
        loadLolSamples(sampleDir.resolve("lol"));
    }

    /**
     * Valorant 5종 샘플 JSON을 DB에 저장.
     * 1. PUUID(계정) 2. Match 3. Lifetime 4. MMR 5. MMR History
     * API 호출 없이 로컬 파일만 사용.
     */
    private void loadValorantSamples(Path valorantDir) {
        String puuid = null;

        // 1. valorant_puuid_sample.json → 계정(PUUID)
        Path puuidFile = valorantDir.resolve("valorant_puuid_sample.json");
        if (Files.isRegularFile(puuidFile)) {
            try {
                String json = Files.readString(puuidFile, StandardCharsets.UTF_8);
                ValorantPuuidApiResponse response = objectMapper.readValue(json, ValorantPuuidApiResponse.class);
                if (valorantAccountService.saveAccount(response) != null) {
                    puuid = response.getData() != null ? response.getData().getPuuid() : null;
                    log.info("[SampleDataLoader] Valorant PUUID(계정) 저장 완료");
                }
            } catch (Exception e) {
                log.error("[SampleDataLoader] Valorant PUUID 샘플 로드 실패", e);
            }
        }

        // 2. valorant_match_sample.json → 매치
        Path matchFile = valorantDir.resolve("valorant_match_sample.json");
        if (Files.isRegularFile(matchFile)) {
            try {
                String json = Files.readString(matchFile, StandardCharsets.UTF_8);
                ValorantMatchDetailDto matchDto = valorantMatchJsonService.parseFirstMatch(json);
                if (matchDto != null && valorantMatchService.saveMatch(matchDto) != null) {
                    log.info("[SampleDataLoader] Valorant Match 저장 완료");
                } else if (matchDto != null) {
                    log.debug("[SampleDataLoader] Valorant Match 이미 존재");
                }
            } catch (ValorantMatchJsonParseException e) {
                log.error("[SampleDataLoader] Valorant Match JSON 파싱 실패: {}", e.getMessage());
            } catch (Exception e) {
                log.error("[SampleDataLoader] Valorant Match 샘플 로드 실패", e);
            }
        }

        // 3. valorant_lifetime_sample.json → Lifetime
        Path lifetimeFile = valorantDir.resolve("valorant_lifetime_sample.json");
        if (Files.isRegularFile(lifetimeFile)) {
            try {
                String json = Files.readString(lifetimeFile, StandardCharsets.UTF_8);
                var dtos = valorantLifetimeJsonService.parseDataFromApiResponse(json);
                int saved = valorantLifetimeService.saveRecords(dtos, 0);
                log.info("[SampleDataLoader] Valorant Lifetime {} 건 저장 완료", saved);
            } catch (ValorantLifetimeJsonService.ValorantLifetimeJsonParseException e) {
                log.error("[SampleDataLoader] Valorant Lifetime JSON 파싱 실패: {}", e.getMessage());
            } catch (Exception e) {
                log.error("[SampleDataLoader] Valorant Lifetime 샘플 로드 실패", e);
            }
        }

        // 4. valorant_mmr_sample.json → MMR
        Path mmrFile = valorantDir.resolve("valorant_mmr_sample.json");
        if (Files.isRegularFile(mmrFile)) {
            try {
                String json = Files.readString(mmrFile, StandardCharsets.UTF_8);
                ValorantMmrApiResponse response = objectMapper.readValue(json, ValorantMmrApiResponse.class);
                if (valorantMmrService.saveMmr(response) != null) {
                    puuid = response.getData() != null ? response.getData().getPuuid() : puuid;
                    log.info("[SampleDataLoader] Valorant MMR 저장 완료");
                }
            } catch (Exception e) {
                log.error("[SampleDataLoader] Valorant MMR 샘플 로드 실패", e);
            }
        }

        // 5. valorant_mmr_history_sample.json → MMR History (puuid 필요)
        Path mmrHistoryFile = valorantDir.resolve("valorant_mmr_history_sample.json");
        if (Files.isRegularFile(mmrHistoryFile) && puuid != null && !puuid.isBlank()) {
            try {
                String json = Files.readString(mmrHistoryFile, StandardCharsets.UTF_8);
                ValorantMmrHistoryApiResponse response = objectMapper.readValue(json, ValorantMmrHistoryApiResponse.class);
                int saved = valorantMmrHistoryService.saveRecords(puuid, response, 0);
                log.info("[SampleDataLoader] Valorant MMR History {} 건 저장 완료", saved);
            } catch (Exception e) {
                log.error("[SampleDataLoader] Valorant MMR History 샘플 로드 실패", e);
            }
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
