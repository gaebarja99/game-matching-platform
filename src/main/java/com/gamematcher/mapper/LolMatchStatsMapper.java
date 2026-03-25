package com.gamematcher.mapper;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import com.gamematcher.dto.ai.evaluation.LolLaneComparisonDto;
import com.gamematcher.dto.ai.evaluation.LolTimelineSnapshotDto;
import com.gamematcher.dto.ai.evaluation.LolPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.LolStatsDTO;
import com.gamematcher.dto.ai.evaluation.LolTimelineEventSummaryDTO;
import com.gamematcher.constant.lol.LolItemNames;
import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.lol.LolMatchTimelineDetailDto;
import com.gamematcher.dto.lol.LolParticipantDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LoL 매치 DTO → 플레이어별 스탯 DTO 변환.
 * LolMatchDetailDto를 LolPlayerMatchStatsDTO 목록으로 변환하며,
 * 선택적으로 LolMatchTimelineDetailDto에서 이벤트 요약을 추출할 수 있다.
 */
@Component
public class LolMatchStatsMapper {

    private static final String GAME_LOL = "LEAGUE_OF_LEGENDS";

    /**
     * 매치만 사용해 플레이어별 스탯 DTO 목록 생성.
     */
    public List<LolPlayerMatchStatsDTO> toPlayerMatchStatsDtos(LolMatchDetailDto matchDto) {
        return toPlayerMatchStatsDtos(matchDto, null);
    }

    /**
     * 매치 + 타임라인으로 플레이어별 스탯 DTO 목록 생성.
     * timelineDto가 null이면 타임라인 이벤트는 비어 있다.
     */
    public List<LolPlayerMatchStatsDTO> toPlayerMatchStatsDtos(
            LolMatchDetailDto matchDto,
            LolMatchTimelineDetailDto timelineDto) {
        if (matchDto == null || matchDto.getInfo() == null
                || matchDto.getInfo().getParticipants() == null) {
            return List.of();
        }

        String matchId = matchDto.getMetadata() != null
                ? matchDto.getMetadata().getMatchId()
                : null;
        long gameDurationSec = nullToZero(matchDto.getInfo().getGameDuration());
        int gameDurationMinutes = (int) Math.max(1, gameDurationSec / 60);

        List<LolPlayerMatchStatsDTO> result = new ArrayList<>();
        for (LolParticipantDto p : matchDto.getInfo().getParticipants()) {
            LolStatsDTO matchStats = toLolStatsDto(p, gameDurationMinutes);
            LolPlayerMatchStatsDTO dto = LolPlayerMatchStatsDTO.builder()
                    .matchId(matchId)
                    .playerPuuid(p.getPuuid())
                    .playerDisplayName(formatDisplayName(p.getRiotIdGameName(), p.getRiotIdTagline()))
                    .teamPosition(coalesce(p.getTeamPosition(), p.getIndividualPosition(), "UNKNOWN"))
                    .champion(p.getChampionName())
                    .matchStats(matchStats)
                    .timelineEvents(timelineDto != null
                            ? extractTimelineEventsForParticipant(matchDto, timelineDto, p)
                            : List.of())
                    .laneComparison(extractLaneComparison(matchDto, timelineDto, p))
                    .build();
            result.add(dto);
        }
        return result;
    }

    /**
     * 특정 puuid의 플레이어 스탯만 반환.
     */
    public LolPlayerMatchStatsDTO toPlayerMatchStatsDto(
            LolMatchDetailDto matchDto,
            String puuid,
            LolMatchTimelineDetailDto timelineDto) {
        if (puuid == null) return null;
        return toPlayerMatchStatsDtos(matchDto, timelineDto).stream()
                .filter(d -> puuid.equals(d.getPlayerPuuid()))
                .findFirst()
                .orElse(null);
    }

    private LolStatsDTO toLolStatsDto(LolParticipantDto p, int gameDurationMinutes) {
        int kills = nullToZero(p.getKills());
        int deaths = nullToZero(p.getDeaths());
        int assists = nullToZero(p.getAssists());
        MatchResult result = Boolean.TRUE.equals(p.getWin()) ? MatchResult.VICTORY : MatchResult.DEFEAT;
        int gold = nullToZero(p.getGoldEarned());
        int cs = nullToZero(p.getTotalMinionsKilled()) + nullToZero(p.getNeutralMinionsKilled());
        long damage = nullToZero(p.getTotalDamageDealtToChampions());
        int vision = nullToZero(p.getVisionScore());

        return new LolStatsDTO(
                GAME_LOL, kills, deaths, assists, result,
                gold, cs, damage, vision, gameDurationMinutes
        );
    }

    private List<LolTimelineEventSummaryDTO> extractTimelineEventsForParticipant(
            LolMatchDetailDto matchDto, LolMatchTimelineDetailDto timelineDto, LolParticipantDto p) {
        if (timelineDto == null || timelineDto.getInfo() == null
                || timelineDto.getInfo().getFrames() == null || p == null) {
            return List.of();
        }
        Integer participantId = p.getParticipantId();
        if (participantId == null) return List.of();

        List<LolTimelineEventSummaryDTO> events = new ArrayList<>();
        Set<Integer> keyItemIds = getKeyItemIds();
        Integer ourTeam = p.getTeamId();
        Map<Integer, String> participantIdToPosition = buildParticipantIdToPosition(matchDto);
        Map<Integer, String> participantIdToChampion = buildParticipantIdToChampion(matchDto);
        int lastDragonKillerTeam = 0; // 100 or 200, for DRAGON_SOUL_GIVEN 추론

        for (var frame : timelineDto.getInfo().getFrames()) {
            if (frame.getEvents() == null) continue;
            long frameTimestamp = frame.getEvents().stream()
                    .filter(e -> e.getTimestamp() != null)
                    .mapToLong(LolMatchTimelineDetailDto.Info.TimelineEvent::getTimestamp)
                    .findFirst()
                    .orElse(0L);
            int minute = (int) (frameTimestamp / 60_000);

            for (var evt : frame.getEvents()) {
                if (evt == null || evt.getType() == null) continue;

                String type = evt.getType();
                if ("CHAMPION_KILL".equals(type)) {
                    if (!isParticipantInvolvedInKill(evt, participantId)) continue;
                    String role = resolvePlayerRole(evt, participantId);
                    String posBrief = formatPositionBrief(evt.getPosition());
                    String detail;
                    if ("사망".equals(role)) {
                        detail = resolveDeathInvolvement(evt, ourTeam, participantIdToPosition);
                    } else if ("킬".equals(role) || "어시스트".equals(role)) {
                        detail = resolveVictimInfo(evt, ourTeam, participantIdToPosition, participantIdToChampion);
                    } else {
                        detail = null;
                    }
                    events.add(LolTimelineEventSummaryDTO.builder()
                            .type(type).timestamp(frameTimestamp).minute(minute)
                            .participantId(participantId).playerRole(role).positionBrief(posBrief)
                            .detail(detail)
                            .build());
                } else if ("ITEM_PURCHASED".equals(type) && participantId.equals(evt.getParticipantId())
                        && evt.getItemId() != null && keyItemIds.contains(evt.getItemId())) {
                    String itemDisplay = LolItemNames.getDisplayName(evt.getItemId()) + " 완성";
                    events.add(LolTimelineEventSummaryDTO.builder()
                            .type(type).timestamp(frameTimestamp).minute(minute)
                            .participantId(participantId).detail(itemDisplay)
                            .build());
                } else if ("TURRET_PLATE_DESTROYED".equals(type) && ourTeam != null && evt.getTeamId() != null) {
                    long evtTs = evt.getTimestamp() != null ? evt.getTimestamp() : frameTimestamp;
                    if (evtTs >= 840_000) continue; // 14분 이후 포탑 방패 소멸
                    int evtMinute = (int) (evtTs / 60_000);
                    String lane = evt.getLaneType() != null ? evt.getLaneType() : "UNKNOWN";
                    boolean isOurTower = ourTeam.equals(evt.getTeamId());
                    String owner = isOurTower ? "ALLY" : "ENEMY";
                    events.add(LolTimelineEventSummaryDTO.builder()
                            .type(type).timestamp(evtTs).minute(evtMinute)
                            .groupKey(owner + "_" + evtMinute + "_" + lane).detail(owner + ":" + lane)
                            .build());
                } else if ("ELITE_MONSTER_KILL".equals(type)) {
                    int killerTeam = evt.getKillerId() != null && evt.getKillerId() >= 6 ? 200 : 100;
                    if ("DRAGON".equals(evt.getMonsterType())) lastDragonKillerTeam = killerTeam;
                    String detail = buildEliteMonsterDetail(evt, ourTeam, killerTeam);
                    events.add(LolTimelineEventSummaryDTO.builder()
                            .type(type).timestamp(frameTimestamp).minute(minute)
                            .participantId(evt.getKillerId()).detail(detail)
                            .build());
                } else if ("BUILDING_KILL".equals(type)) {
                    String detail = buildBuildingKillDetail(evt, ourTeam);
                    events.add(LolTimelineEventSummaryDTO.builder()
                            .type(type).timestamp(frameTimestamp).minute(minute)
                            .participantId(evt.getKillerId()).detail(detail)
                            .build());
                } else if ("DRAGON_SOUL_GIVEN".equals(type)) {
                    // Riot API 15.1+ 버그: teamId=0이면 2용 시 영혼 속성 결정(실제 영혼 아님), 100/200이면 4용 실제 영혼 획득
                    Integer evtTeamId = evt.getTeamId();
                    String detail = buildDragonSoulDetail(evt, ourTeam, evtTeamId);
                    events.add(LolTimelineEventSummaryDTO.builder()
                            .type(type).timestamp(frameTimestamp).minute(minute)
                            .detail(detail)
                            .build());
                } else if ("GAME_END".equals(type) && evt.getWinningTeam() != null) {
                    long evtTs = evt.getTimestamp() != null ? evt.getTimestamp() : frameTimestamp;
                    int evtMinute = (int) (evtTs / 60_000);
                    String owner = (ourTeam != null && evt.getWinningTeam().equals(ourTeam)) ? "ALLY" : "ENEMY";
                    events.add(LolTimelineEventSummaryDTO.builder()
                            .type(type).timestamp(evtTs).minute(evtMinute)
                            .detail(owner)
                            .build());
                } else if ("LEVEL_UP".equals(type) && participantId.equals(evt.getParticipantId())
                        && evt.getLevel() != null && evt.getLevel() == 6) {
                    long evtTs = evt.getTimestamp() != null ? evt.getTimestamp() : frameTimestamp;
                    int evtMinute = (int) (evtTs / 60_000);
                    events.add(LolTimelineEventSummaryDTO.builder()
                            .type(type).timestamp(evtTs).minute(evtMinute)
                            .participantId(participantId)
                            .detail("6레벨 달성")
                            .build());
                }
            }
        }
        return events;
    }

    private Map<Integer, String> buildParticipantIdToPosition(LolMatchDetailDto matchDto) {
        Map<Integer, String> map = new HashMap<>();
        if (matchDto == null || matchDto.getInfo() == null || matchDto.getInfo().getParticipants() == null) {
            return map;
        }
        for (LolParticipantDto pp : matchDto.getInfo().getParticipants()) {
            if (pp.getParticipantId() != null) {
                String pos = coalesce(pp.getTeamPosition(), pp.getIndividualPosition());
                map.put(pp.getParticipantId(), pos != null ? pos.toUpperCase() : "UNKNOWN");
            }
        }
        return map;
    }

    private Map<Integer, String> buildParticipantIdToChampion(LolMatchDetailDto matchDto) {
        Map<Integer, String> map = new HashMap<>();
        if (matchDto == null || matchDto.getInfo() == null || matchDto.getInfo().getParticipants() == null) {
            return map;
        }
        for (LolParticipantDto pp : matchDto.getInfo().getParticipants()) {
            if (pp.getParticipantId() != null && pp.getChampionName() != null) {
                map.put(pp.getParticipantId(), pp.getChampionName());
            }
        }
        return map;
    }

    /**
     * 킬/어시스트 시 피해자(누가 죽었는지) 정보.
     * "적 탑(사이온)", "적 미드(아리)" 등 형식.
     */
    private String resolveVictimInfo(
            LolMatchTimelineDetailDto.Info.TimelineEvent evt,
            Integer ourTeam,
            Map<Integer, String> participantIdToPosition,
            Map<Integer, String> participantIdToChampion) {
        if (ourTeam == null) return null;
        Integer victimId = evt.getVictimId();
        if (victimId == null) return null;
        int victimTeam = victimId <= 5 ? 100 : 200;
        if (victimTeam == ourTeam) return null; // 아군이 죽은 경우(팀킬) 제외
        String pos = participantIdToPosition.getOrDefault(victimId, "UNKNOWN");
        String posKr = positionToKorean(pos);
        String champ = participantIdToChampion != null ? participantIdToChampion.get(victimId) : null;
        return champ != null ? "적 " + posKr + "(" + champ + ")" : "적 " + posKr;
    }

    private String positionToKorean(String pos) {
        if (pos == null) return "?";
        return switch (pos.toUpperCase()) {
            case "TOP" -> "탑";
            case "MID", "MIDDLE" -> "미드";
            case "JUNGLE", "JGL" -> "정글";
            case "BOT", "BOTTOM", "ADC" -> "봇";
            case "SUPPORT", "SUP", "UTILITY" -> "서포터";
            default -> pos;
        };
    }

    /**
     * 사망 시 킬을 넣은 상대가 라이너인지 정글러인지 판정.
     * "적 정글 개입" = 킬러 또는 어시스트에 적 정글러 포함
     * "적 라이너 솔로킬" = 킬러가 적 라이너이고 정글 개입 없음
     */
    private String resolveDeathInvolvement(
            LolMatchTimelineDetailDto.Info.TimelineEvent evt,
            Integer ourTeam,
            Map<Integer, String> participantIdToPosition) {
        if (ourTeam == null || participantIdToPosition == null) return null;
        Integer killerId = evt.getKillerId();
        if (killerId == null || killerId == 0) return null; // 몬스터/포탑 킬
        int killerTeam = killerId <= 5 ? 100 : 200;
        if (killerTeam == ourTeam) return null; // 아군에게 죽은 경우(팀킬 등) 제외
        String killerPos = participantIdToPosition.getOrDefault(killerId, "UNKNOWN");
        boolean killerIsJungle = "JUNGLE".equals(killerPos) || "JGL".equals(killerPos);
        boolean enemyJungleInvolved = killerIsJungle;
        if (!enemyJungleInvolved && evt.getAssistingParticipantIds() != null) {
            for (Integer aid : evt.getAssistingParticipantIds()) {
                if (aid == null) continue;
                int aTeam = aid <= 5 ? 100 : 200;
                if (aTeam != ourTeam) {
                    String aPos = participantIdToPosition.getOrDefault(aid, "UNKNOWN");
                    if ("JUNGLE".equals(aPos) || "JGL".equals(aPos)) {
                        enemyJungleInvolved = true;
                        break;
                    }
                }
            }
        }
        return enemyJungleInvolved ? "적 정글 개입" : "적 라이너 솔로킬";
    }

    private boolean isParticipantInvolvedInKill(LolMatchTimelineDetailDto.Info.TimelineEvent evt, Integer participantId) {
        if (participantId.equals(evt.getKillerId())) return true;
        if (participantId.equals(evt.getVictimId())) return true;
        if (evt.getAssistingParticipantIds() != null && evt.getAssistingParticipantIds().contains(participantId)) {
            return true;
        }
        return false;
    }

    private String resolvePlayerRole(LolMatchTimelineDetailDto.Info.TimelineEvent evt, Integer participantId) {
        if (participantId.equals(evt.getKillerId())) return "킬";
        if (participantId.equals(evt.getVictimId())) return "사망";
        if (evt.getAssistingParticipantIds() != null && evt.getAssistingParticipantIds().contains(participantId)) {
            return "어시스트";
        }
        return null;
    }

    private String formatPositionBrief(LolMatchTimelineDetailDto.Info.ParticipantFrame.Position pos) {
        if (pos == null || pos.getX() == null || pos.getY() == null) return null;
        int x = pos.getX(), y = pos.getY();
        if (x < 5000 && y < 5000) return "아군본진";
        if (x > 12000 || y > 12000) return "상대본진";
        if (x > 8000 && y > 8000) return "상대정글/바론";
        if (x < 4000 && y < 4000) return "아군정글/용";
        return "중앙/라인";
    }

    private Set<Integer> getKeyItemIds() {
        Set<Integer> ids = new HashSet<>();
        ids.add(3089); ids.add(3157); ids.add(3190); ids.add(3124); ids.add(3078);
        ids.add(6692); ids.add(6693); ids.add(6694);
        ids.add(3036); ids.add(3074); ids.add(3748);
        ids.add(3085); ids.add(3153);
        ids.add(3003); ids.add(3100); ids.add(3116);
        ids.add(2065); ids.add(6617);
        ids.add(6662); ids.add(6664); ids.add(6676);
        return ids;
    }

    /** ELITE_MONSTER_KILL: "ALLY:바론" / "ENEMY:드래곤(화염)" / "ENEMY:공허유충" 등 */
    private String buildEliteMonsterDetail(LolMatchTimelineDetailDto.Info.TimelineEvent evt, Integer ourTeam, int killerTeam) {
        String owner = (ourTeam != null && killerTeam == ourTeam) ? "ALLY" : "ENEMY";
        String monsterKr = monsterTypeToKorean(evt.getMonsterType(), evt.getMonsterSubType());
        return owner + ":" + monsterKr;
    }

    /** BUILDING_KILL: evt.teamId=파괴당한 포탑 소유팀. "ALLY:봇:1차"=우리 포탑 파괴됨, "ENEMY:미드:억제기"=적 포탑 파괴됨 */
    private String buildBuildingKillDetail(LolMatchTimelineDetailDto.Info.TimelineEvent evt, Integer ourTeam) {
        Integer ownerTeam = evt.getTeamId();
        boolean isOurTowerDestroyed = ourTeam != null && ourTeam.equals(ownerTeam);
        String owner = isOurTowerDestroyed ? "ALLY" : "ENEMY";
        String lane = evt.getLaneType() != null ? laneTypeToKorean(evt.getLaneType()) : "";
        String tier = towerTypeToKorean(evt.getTowerType(), evt.getBuildingType());
        return owner + ":" + lane + ":" + tier;
    }

    /** DRAGON_SOUL_GIVEN: teamId=0이면 영혼 속성 결정(2용), 100/200이면 실제 영혼 획득(4용) */
    private String buildDragonSoulDetail(LolMatchTimelineDetailDto.Info.TimelineEvent evt, Integer ourTeam, Integer evtTeamId) {
        String soulKr = dragonSoulNameToKorean(evt.getName());
        if (evtTeamId == null || evtTeamId == 0) {
            return "SOUL_TYPE:" + soulKr; // Riot 15.1+ 버그: 2용 시 속성만 결정, 실제 영혼 아님
        }
        String owner = (ourTeam != null && evtTeamId == ourTeam) ? "ALLY" : "ENEMY";
        return owner + ":" + soulKr;
    }

    private String monsterTypeToKorean(String monsterType, String monsterSubType) {
        if (monsterType == null) return "엘리트몬스터";
        return switch (monsterType) {
            case "DRAGON" -> {
                String sub = monsterSubType != null ? dragonSubTypeToKorean(monsterSubType) : "";
                yield "드래곤" + (sub.isEmpty() ? "" : "(" + sub + ")");
            }
            case "BARON_NASHOR" -> "바론";
            case "RIFTHERALD" -> "전령";
            case "HORDE" -> "공허유충";
            default -> monsterType;
        };
    }

    private String dragonSubTypeToKorean(String sub) {
        if (sub == null) return "";
        return switch (sub) {
            case "FIRE_DRAGON" -> "화염";
            case "WATER_DRAGON", "OCEAN_DRAGON" -> "바다";
            case "EARTH_DRAGON", "MOUNTAIN_DRAGON" -> "대지";
            case "AIR_DRAGON", "CLOUD_DRAGON" -> "구름";
            case "HEXTECH_DRAGON" -> "헥스테크";
            case "CHEMTECH_DRAGON" -> "첸테크";
            default -> sub.replace("_DRAGON", "").toLowerCase();
        };
    }

    private String towerTypeToKorean(String towerType, String buildingType) {
        if ("INHIBITOR_BUILDING".equals(buildingType)) return "억제기";
        if (towerType == null) return "포탑";
        return switch (towerType) {
            case "OUTER_TURRET" -> "1차";
            case "INNER_TURRET" -> "2차";
            case "BASE_TURRET" -> "억제기포탑";
            case "NEXUS_TURRET" -> "넥서스";
            default -> towerType.toLowerCase().replace("_", "");
        };
    }

    private String laneTypeToKorean(String lane) {
        if (lane == null) return "";
        return switch (lane.toUpperCase()) {
            case "TOP_LANE" -> "탑";
            case "MID_LANE", "MIDDLE_LANE" -> "미드";
            case "BOT_LANE", "BOTTOM_LANE" -> "봇";
            default -> lane;
        };
    }

    private String dragonSoulNameToKorean(String name) {
        if (name == null || name.isBlank()) return "영혼";
        return switch (name.toUpperCase()) {
            case "MOUNTAIN", "EARTH" -> "산";
            case "INFERNAL", "FIRE" -> "화염";
            case "OCEAN", "WATER" -> "바다";
            case "CLOUD", "AIR" -> "구름";
            case "HEXTECH" -> "헥스테크";
            case "CHEMTECH" -> "첸테크";
            default -> name;
        };
    }

    private static final int[] SNAPSHOT_MINUTES = {5, 10, 15, 20, 25};

    private LolLaneComparisonDto extractLaneComparison(
            LolMatchDetailDto matchDto, LolMatchTimelineDetailDto timelineDto, LolParticipantDto p) {
        LolLaneComparisonDto dto = new LolLaneComparisonDto();

        if (p.getChallenges() != null) {
            Object maxCs = p.getChallenges().get("maxCsAdvantageOnLaneOpponent");
            Object goldExp = p.getChallenges().get("laningPhaseGoldExpAdvantage");
            if (maxCs instanceof Number) dto.setMaxCsAdvantageOnLaneOpponent(((Number) maxCs).doubleValue());
            if (goldExp instanceof Number) dto.setLaningPhaseGoldExpAdvantage(((Number) goldExp).doubleValue());
        }

        if (timelineDto != null && timelineDto.getInfo() != null && timelineDto.getInfo().getFrames() != null) {
            var frames = timelineDto.getInfo().getFrames();
            int pid = nullToZero(p.getParticipantId());
            int myTeam = nullToZero(p.getTeamId());
            int oppPid = getLaneOpponentId(pid, myTeam);

            List<LolTimelineSnapshotDto> snapshots = new ArrayList<>();
            for (int minute : SNAPSHOT_MINUTES) {
                if (minute >= frames.size()) break;
                var pf = getParticipantFrameAtMinute(frames, minute);
                if (pf == null) continue;

                int myGold = getGold(pf, pid);
                int oppGold = getGold(pf, oppPid);
                int myCs = getCs(pf, pid);
                int oppCs = getCs(pf, oppPid);
                int myXp = getXp(pf, pid);
                int oppXp = getXp(pf, oppPid);

                int team100Gold = getTeamGold(pf, 100);
                int team200Gold = getTeamGold(pf, 200);
                int team100Cs = getTeamCs(pf, 100);
                int team200Cs = getTeamCs(pf, 200);
                int team100Xp = getTeamXp(pf, 100);
                int team200Xp = getTeamXp(pf, 200);

                int goldDiffTeam = myTeam == 100 ? team100Gold - team200Gold : team200Gold - team100Gold;
                int csDiffTeam = myTeam == 100 ? team100Cs - team200Cs : team200Cs - team100Cs;
                int xpDiffTeam = myTeam == 100 ? team100Xp - team200Xp : team200Xp - team100Xp;

                snapshots.add(LolTimelineSnapshotDto.builder()
                        .minute(minute)
                        .goldDiffLane(myGold - oppGold)
                        .csDiffLane(myCs - oppCs)
                        .xpDiffLane(myXp - oppXp)
                        .goldDiffTeam(goldDiffTeam)
                        .csDiffTeam(csDiffTeam)
                        .xpDiffTeam(xpDiffTeam)
                        .build());
            }
            dto.setSnapshots(snapshots);

            if (frames.size() > 10) {
                var pf10 = getParticipantFrameAtMinute(frames, 10);
                var pf15 = frames.size() > 15 ? getParticipantFrameAtMinute(frames, 15) : null;
                if (pf10 != null) {
                    dto.setGoldDiffAt10(getGold(pf10, pid) - getGold(pf10, oppPid));
                    dto.setCsDiffAt10(getCs(pf10, pid) - getCs(pf10, oppPid));
                    dto.setXpDiffAt10(getXp(pf10, pid) - getXp(pf10, oppPid));
                }
                if (pf15 != null) {
                    dto.setGoldDiffAt15(getGold(pf15, pid) - getGold(pf15, oppPid));
                    dto.setCsDiffAt15(getCs(pf15, pid) - getCs(pf15, oppPid));
                    dto.setXpDiffAt15(getXp(pf15, pid) - getXp(pf15, oppPid));
                }
            }
        }
        return dto;
    }

    private int getTeamGold(Map<String, LolMatchTimelineDetailDto.Info.ParticipantFrame> pf, int teamId) {
        if (pf == null) return 0;
        int start = teamId == 100 ? 1 : 6;
        int sum = 0;
        for (int i = start; i < start + 5; i++) {
            sum += getGold(pf, i);
        }
        return sum;
    }

    private int getTeamCs(Map<String, LolMatchTimelineDetailDto.Info.ParticipantFrame> pf, int teamId) {
        if (pf == null) return 0;
        int start = teamId == 100 ? 1 : 6;
        int sum = 0;
        for (int i = start; i < start + 5; i++) {
            sum += getCs(pf, i);
        }
        return sum;
    }

    private int getTeamXp(Map<String, LolMatchTimelineDetailDto.Info.ParticipantFrame> pf, int teamId) {
        if (pf == null) return 0;
        int start = teamId == 100 ? 1 : 6;
        int sum = 0;
        for (int i = start; i < start + 5; i++) {
            sum += getXp(pf, i);
        }
        return sum;
    }

    private Map<String, LolMatchTimelineDetailDto.Info.ParticipantFrame> getParticipantFrameAtMinute(
            List<LolMatchTimelineDetailDto.Info.Frame> frames, int minute) {
        if (minute < 0 || minute >= frames.size()) return null;
        return frames.get(minute).getParticipantFrames();
    }

    private int getGold(Map<String, LolMatchTimelineDetailDto.Info.ParticipantFrame> pf, int participantId) {
        if (pf == null) return 0;
        var frame = pf.get(String.valueOf(participantId));
        return frame != null && frame.getTotalGold() != null ? frame.getTotalGold() : 0;
    }

    private int getCs(Map<String, LolMatchTimelineDetailDto.Info.ParticipantFrame> pf, int participantId) {
        if (pf == null) return 0;
        var frame = pf.get(String.valueOf(participantId));
        if (frame == null) return 0;
        int mn = nullToZero(frame.getMinionsKilled());
        int jg = nullToZero(frame.getJungleMinionsKilled());
        return mn + jg;
    }

    private int getXp(Map<String, LolMatchTimelineDetailDto.Info.ParticipantFrame> pf, int participantId) {
        if (pf == null) return 0;
        var frame = pf.get(String.valueOf(participantId));
        return frame != null && frame.getXp() != null ? frame.getXp() : 0;
    }

    private int getLaneOpponentId(int participantId, int myTeam) {
        if (myTeam == 100) return participantId + 5;
        if (myTeam == 200) return participantId - 5;
        return participantId <= 5 ? participantId + 5 : participantId - 5;
    }

    private String formatDisplayName(String gameName, String tagline) {
        if (gameName == null && tagline == null) return null;
        if (tagline == null || tagline.isBlank()) return gameName;
        return (gameName != null ? gameName : "") + "#" + tagline;
    }

    private String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private int nullToZero(Integer v) {
        return v != null ? v : 0;
    }

    private long nullToZero(Long v) {
        return v != null ? v : 0L;
    }
}
