package com.gamematcher.mapper;

import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.dto.valorant.ValorantMatchInfoDto;
import com.gamematcher.dto.valorant.ValorantPlayerDto;
import com.gamematcher.entity.match.valorant.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Valorant 전적 DTO ↔ Entity 양방향 변환 (AI 분석용 상세 매핑)
 */
@Component
public class ValorantMatchMapper {

    /**
     * 매치 상세 DTO → ValorantMatch 엔티티 (플레이어, 라운드, 킬 이벤트 포함)
     */
    public ValorantMatch toEntity(ValorantMatchDetailDto matchDto) {
        if (matchDto == null) return null;

        ValorantMatch match = new ValorantMatch();

        // metadata
        if (matchDto.getMetadata() != null) {
            var meta = matchDto.getMetadata();
            match.setMatchId(meta.getMatchId());
            match.setMap(meta.getMap());
            match.setGameVersion(meta.getGameVersion());
            match.setGameLength(meta.getGameLength());
            match.setGameStart(meta.getGameStart());
            match.setGameStartPatched(meta.getGameStartPatched());
            match.setRoundsPlayed(meta.getRoundsPlayed());
            match.setMode(meta.getMode());
            match.setModeId(meta.getModeId());
            match.setQueue(meta.getQueue());
            match.setSeasonId(meta.getSeasonId());
            match.setPlatform(meta.getPlatform());
            match.setRegion(meta.getRegion());
            match.setCluster(meta.getCluster());
            if (meta.getPremierInfo() != null) {
                match.setPremierTournamentId(meta.getPremierInfo().getTournamentId());
                match.setPremierMatchupId(meta.getPremierInfo().getMatchupId());
            }
        }

        // teams
        if (matchDto.getTeams() != null) {
            if (matchDto.getTeams().getRed() != null) {
                var red = matchDto.getTeams().getRed();
                match.setRedRoundsWon(red.getRoundsWon());
                match.setRedHasWon(red.isHasWon());
            }
            if (matchDto.getTeams().getBlue() != null) {
                var blue = matchDto.getTeams().getBlue();
                match.setBlueRoundsWon(blue.getRoundsWon());
                match.setBlueHasWon(blue.isHasWon());
            }
        }

        // players
        if (matchDto.getPlayers() != null && matchDto.getPlayers().getAllPlayers() != null) {
            for (ValorantPlayerDto p : matchDto.getPlayers().getAllPlayers()) {
                match.getPlayers().add(toPlayerEntity(match, p, matchDto.getTeams()));
            }
        }

        // rounds
        if (matchDto.getRounds() != null) {
            for (int i = 0; i < matchDto.getRounds().size(); i++) {
                match.getRounds().add(toRoundEntity(match, matchDto.getRounds().get(i), i));
            }
        }

        // kills (match-level kills list)
        if (matchDto.getKills() != null) {
            for (ValorantMatchDetailDto.KillEvent ke : matchDto.getKills()) {
                match.getKillEvents().add(toKillEventEntity(match, ke));
            }
        }

        return match;
    }

    private ValorantMatchPlayer toPlayerEntity(ValorantMatch match, ValorantPlayerDto p, ValorantMatchDetailDto.Teams teams) {
        ValorantMatchPlayer entity = new ValorantMatchPlayer();
        entity.setMatch(match);
        entity.setPuuid(p.getPuuid());
        entity.setName(p.getName());
        entity.setTag(p.getTag());
        entity.setTeam(p.getTeam());
        entity.setCharacter(p.getCharacter());
        entity.setLevel(p.getLevel());
        entity.setCurrentTier(p.getCurrentTier());
        entity.setCurrentTierPatched(p.getCurrentTierPatched());

        if (p.getStats() != null) {
            entity.setKills(p.getStats().getKills());
            entity.setDeaths(p.getStats().getDeaths());
            entity.setAssists(p.getStats().getAssists());
            entity.setScore(p.getStats().getScore());
            entity.setHeadshots(p.getStats().getHeadshots());
            entity.setBodyshots(p.getStats().getBodyshots());
            entity.setLegshots(p.getStats().getLegshots());
        }
        entity.setDamageMade(p.getDamageMade());
        entity.setDamageReceived(p.getDamageReceived());

        if (p.getAbilityCasts() != null) {
            entity.setAbilityXCast(p.getAbilityCasts().getXCast());
            entity.setAbilityECast(p.getAbilityCasts().getECast());
            entity.setAbilityQCast(p.getAbilityCasts().getQCast());
            entity.setAbilityCCast(p.getAbilityCasts().getCCast());
        }

        if (p.getEconomy() != null) {
            if (p.getEconomy().getSpent() != null) {
                entity.setEconomySpentOverall(p.getEconomy().getSpent().getOverall());
            }
            if (p.getEconomy().getLoadoutValue() != null) {
                entity.setLoadoutValueOverall(p.getEconomy().getLoadoutValue().getOverall());
            }
        }

        if (p.getBehavior() != null) {
            entity.setAfkRounds(p.getBehavior().getAfkRounds());
            entity.setRoundsInSpawn(p.getBehavior().getRoundsInSpawn());
            if (p.getBehavior().getFriendlyFire() != null) {
                entity.setFriendlyFireIncoming(p.getBehavior().getFriendlyFire().getIncoming());
                entity.setFriendlyFireOutgoing(p.getBehavior().getFriendlyFire().getOutgoing());
            }
        }

        entity.setWin(resolveWin(p.getTeam(), teams));
        return entity;
    }

    private boolean resolveWin(String playerTeam, ValorantMatchDetailDto.Teams teams) {
        if (teams == null || playerTeam == null) return false;
        if ("Red".equalsIgnoreCase(playerTeam) && teams.getRed() != null) return teams.getRed().isHasWon();
        if ("Blue".equalsIgnoreCase(playerTeam) && teams.getBlue() != null) return teams.getBlue().isHasWon();
        return false;
    }

    private ValorantMatchRound toRoundEntity(ValorantMatch match, ValorantMatchDetailDto.Round r, int index) {
        ValorantMatchRound entity = new ValorantMatchRound();
        entity.setMatch(match);
        entity.setRoundIndex(index);
        entity.setWinningTeam(r.getWinningTeam());
        entity.setEndType(r.getEndType());
        entity.setBombPlanted(r.isBombPlanted());
        entity.setBombDefused(r.isBombDefused());

        if (r.getPlantEvents() != null) {
            entity.setPlantSite(r.getPlantEvents().getPlantSite());
            entity.setPlantTimeInRound(r.getPlantEvents().getPlantTimeInRound());
            if (r.getPlantEvents().getPlantedBy() != null) {
                entity.setPlantedByPuuid(r.getPlantEvents().getPlantedBy().getPuuid());
            }
            if (r.getPlantEvents().getPlantLocation() != null) {
                entity.setPlantLocationX(r.getPlantEvents().getPlantLocation().getX());
                entity.setPlantLocationY(r.getPlantEvents().getPlantLocation().getY());
            }
            if (r.getPlantEvents().getPlayerLocationsOnPlant() != null) {
                for (ValorantMatchDetailDto.PlayerLocation pl : r.getPlantEvents().getPlayerLocationsOnPlant()) {
                    entity.getPlayerLocations().add(toPlayerLocationEntity(entity, "PLANT", pl));
                }
            }
        }
        if (r.getDefuseEvents() != null) {
            entity.setDefuseTimeInRound(r.getDefuseEvents().getDefuseTimeInRound());
            if (r.getDefuseEvents().getDefusedBy() != null) {
                entity.setDefusedByPuuid(r.getDefuseEvents().getDefusedBy().getPuuid());
            }
            if (r.getDefuseEvents().getDefuseLocation() != null) {
                entity.setDefuseLocationX(r.getDefuseEvents().getDefuseLocation().getX());
                entity.setDefuseLocationY(r.getDefuseEvents().getDefuseLocation().getY());
            }
            if (r.getDefuseEvents().getPlayerLocationsOnDefuse() != null) {
                for (ValorantMatchDetailDto.PlayerLocation pl : r.getDefuseEvents().getPlayerLocationsOnDefuse()) {
                    entity.getPlayerLocations().add(toPlayerLocationEntity(entity, "DEFUSE", pl));
                }
            }
        }

        if (r.getPlayerStats() != null) {
            for (ValorantMatchDetailDto.RoundPlayerStat ps : r.getPlayerStats()) {
                entity.getPlayerStats().add(toRoundPlayerEntity(entity, ps));
            }
        }
        return entity;
    }

    private ValorantMatchRoundPlayer toRoundPlayerEntity(ValorantMatchRound round, ValorantMatchDetailDto.RoundPlayerStat ps) {
        ValorantMatchRoundPlayer entity = new ValorantMatchRoundPlayer();
        entity.setRound(round);
        entity.setPlayerPuuid(ps.getPlayerPuuid());
        entity.setPlayerDisplayName(ps.getPlayerDisplayName());
        entity.setPlayerTeam(ps.getPlayerTeam());
        entity.setDamage(ps.getDamage());
        entity.setHeadshots(ps.getHeadshots());
        entity.setBodyshots(ps.getBodyshots());
        entity.setLegshots(ps.getLegshots());
        entity.setKills(ps.getKills());
        entity.setScore(ps.getScore());
        if (ps.getAbilityCasts() != null) {
            entity.setAbilityXCasts(ps.getAbilityCasts().getXCasts());
            entity.setAbilityECasts(ps.getAbilityCasts().getECasts());
            entity.setAbilityQCasts(ps.getAbilityCasts().getQCasts());
            entity.setAbilityCCasts(ps.getAbilityCasts().getCCasts());
        }
        if (ps.getEconomy() != null) {
            entity.setLoadoutValue(ps.getEconomy().getLoadoutValue());
            entity.setEconomyRemaining(ps.getEconomy().getRemaining());
            entity.setEconomySpent(ps.getEconomy().getSpent());
        }
        entity.setWasAfk(ps.isWasAfk());
        entity.setWasPenalized(ps.isWasPenalized());
        entity.setStayedInSpawn(ps.isStayedInSpawn());
        if (ps.getDamageEvents() != null) {
            for (ValorantMatchDetailDto.DamageEvent de : ps.getDamageEvents()) {
                entity.getDamageEvents().add(toDamageEventEntity(entity, de));
            }
        }
        return entity;
    }

    private ValorantRoundPlayerLocation toPlayerLocationEntity(ValorantMatchRound round, String eventType, ValorantMatchDetailDto.PlayerLocation pl) {
        ValorantRoundPlayerLocation loc = new ValorantRoundPlayerLocation();
        loc.setRound(round);
        loc.setEventType(eventType);
        loc.setPlayerPuuid(pl.getPlayerPuuid());
        loc.setPlayerDisplayName(pl.getPlayerDisplayName());
        loc.setPlayerTeam(pl.getPlayerTeam());
        if (pl.getLocation() != null) {
            loc.setLocationX(pl.getLocation().getX());
            loc.setLocationY(pl.getLocation().getY());
        }
        loc.setViewRadians(pl.getViewRadians());
        return loc;
    }

    private ValorantRoundPlayerDamageEvent toDamageEventEntity(ValorantMatchRoundPlayer roundPlayer, ValorantMatchDetailDto.DamageEvent de) {
        ValorantRoundPlayerDamageEvent evt = new ValorantRoundPlayerDamageEvent();
        evt.setRoundPlayer(roundPlayer);
        evt.setReceiverPuuid(de.getReceiverPuuid());
        evt.setReceiverDisplayName(de.getReceiverDisplayName());
        evt.setReceiverTeam(de.getReceiverTeam());
        evt.setBodyshots(de.getBodyshots());
        evt.setHeadshots(de.getHeadshots());
        evt.setLegshots(de.getLegshots());
        evt.setDamage(de.getDamage());
        return evt;
    }

    private ValorantKillEvent toKillEventEntity(ValorantMatch match, ValorantMatchDetailDto.KillEvent ke) {
        ValorantKillEvent entity = new ValorantKillEvent();
        entity.setMatch(match);
        entity.setRoundNumber(ke.getRound());
        entity.setKillTimeInRound(ke.getKillTimeInRound());
        entity.setKillTimeInMatch(ke.getKillTimeInMatch());
        entity.setKillerPuuid(ke.getKillerPuuid());
        entity.setKillerDisplayName(ke.getKillerDisplayName());
        entity.setKillerTeam(ke.getKillerTeam());
        entity.setVictimPuuid(ke.getVictimPuuid());
        entity.setVictimDisplayName(ke.getVictimDisplayName());
        entity.setVictimTeam(ke.getVictimTeam());
        if (ke.getVictimDeathLocation() != null) {
            entity.setVictimDeathX(ke.getVictimDeathLocation().getX());
            entity.setVictimDeathY(ke.getVictimDeathLocation().getY());
        }
        entity.setDamageWeaponId(ke.getDamageWeaponId());
        entity.setDamageWeaponName(ke.getDamageWeaponName());
        entity.setSecondaryFireMode(ke.isSecondaryFireMode());

        if (ke.getAssistants() != null) {
            for (ValorantMatchDetailDto.Assistant a : ke.getAssistants()) {
                ValorantKillAssistant ass = new ValorantKillAssistant();
                ass.setKillEvent(entity);
                ass.setAssistantPuuid(a.getAssistantPuuid());
                ass.setAssistantDisplayName(a.getAssistantDisplayName());
                ass.setAssistantTeam(a.getAssistantTeam());
                entity.getAssistants().add(ass);
            }
        }
        return entity;
    }

    /**
     * ValorantMatch 엔티티 → 매치 상세 DTO (플레이어, 라운드, 킬 이벤트 포함)
     * DB에서 꺼낸 엔티티를 API 응답용 DTO로 변환
     */
    public ValorantMatchDetailDto toDto(ValorantMatch match) {
        if (match == null) return null;

        ValorantMatchDetailDto dto = new ValorantMatchDetailDto();
        dto.setAvailable(true);

        // metadata
        dto.setMetadata(toMetadataDto(match));

        // teams
        dto.setTeams(toTeamsDto(match));

        // players (allPlayers + red/blue 분리)
        dto.setPlayers(toPlayersWrapperDto(match));

        // rounds
        dto.setRounds(toRoundsDto(match));

        // kills
        dto.setKills(toKillEventsDto(match));

        dto.setObservers(Collections.emptyList());
        dto.setCoaches(Collections.emptyList());

        return dto;
    }

    private ValorantMatchInfoDto toMetadataDto(ValorantMatch match) {
        ValorantMatchInfoDto meta = new ValorantMatchInfoDto();
        meta.setMatchId(match.getMatchId());
        meta.setMap(match.getMap());
        meta.setGameVersion(match.getGameVersion());
        meta.setGameLength(match.getGameLength() != null ? match.getGameLength() : 0);
        meta.setGameStart(match.getGameStart() != null ? match.getGameStart() : 0L);
        meta.setGameStartPatched(match.getGameStartPatched());
        meta.setRoundsPlayed(match.getRoundsPlayed() != null ? match.getRoundsPlayed() : 0);
        meta.setMode(match.getMode());
        meta.setModeId(match.getModeId());
        meta.setQueue(match.getQueue());
        meta.setSeasonId(match.getSeasonId());
        meta.setPlatform(match.getPlatform());
        meta.setRegion(match.getRegion());
        meta.setCluster(match.getCluster());
        if (match.getPremierTournamentId() != null || match.getPremierMatchupId() != null) {
            ValorantMatchInfoDto.PremierInfo premier = new ValorantMatchInfoDto.PremierInfo();
            premier.setTournamentId(match.getPremierTournamentId());
            premier.setMatchupId(match.getPremierMatchupId());
            meta.setPremierInfo(premier);
        }
        return meta;
    }

    private ValorantMatchDetailDto.Teams toTeamsDto(ValorantMatch match) {
        ValorantMatchDetailDto.Teams teams = new ValorantMatchDetailDto.Teams();
        if (match.getRedRoundsWon() != null || match.getRedHasWon() != null) {
            ValorantMatchDetailDto.TeamResult red = new ValorantMatchDetailDto.TeamResult();
            red.setRoundsWon(match.getRedRoundsWon() != null ? match.getRedRoundsWon() : 0);
            red.setRoundsLost(match.getBlueRoundsWon() != null ? match.getBlueRoundsWon() : 0);
            red.setHasWon(Boolean.TRUE.equals(match.getRedHasWon()));
            teams.setRed(red);
        }
        if (match.getBlueRoundsWon() != null || match.getBlueHasWon() != null) {
            ValorantMatchDetailDto.TeamResult blue = new ValorantMatchDetailDto.TeamResult();
            blue.setRoundsWon(match.getBlueRoundsWon() != null ? match.getBlueRoundsWon() : 0);
            blue.setRoundsLost(match.getRedRoundsWon() != null ? match.getRedRoundsWon() : 0);
            blue.setHasWon(Boolean.TRUE.equals(match.getBlueHasWon()));
            teams.setBlue(blue);
        }
        return teams;
    }

    private ValorantMatchDetailDto.PlayersWrapper toPlayersWrapperDto(ValorantMatch match) {
        List<ValorantMatchPlayer> players = match.getPlayers();
        if (players == null) players = Collections.emptyList();

        List<ValorantPlayerDto> allPlayers = players.stream()
                .map(this::toPlayerDto)
                .collect(Collectors.toList());

        List<ValorantPlayerDto> red = players.stream()
                .filter(p -> "Red".equalsIgnoreCase(p.getTeam()))
                .map(this::toPlayerDto)
                .collect(Collectors.toList());

        List<ValorantPlayerDto> blue = players.stream()
                .filter(p -> "Blue".equalsIgnoreCase(p.getTeam()))
                .map(this::toPlayerDto)
                .collect(Collectors.toList());

        ValorantMatchDetailDto.PlayersWrapper wrapper = new ValorantMatchDetailDto.PlayersWrapper();
        wrapper.setAllPlayers(allPlayers);
        wrapper.setRed(red);
        wrapper.setBlue(blue);
        return wrapper;
    }

    private ValorantPlayerDto toPlayerDto(ValorantMatchPlayer p) {
        ValorantPlayerDto dto = new ValorantPlayerDto();
        dto.setPuuid(p.getPuuid());
        dto.setName(p.getName());
        dto.setTag(p.getTag());
        dto.setTeam(p.getTeam());
        dto.setCharacter(p.getCharacter());
        dto.setLevel(p.getLevel() != null ? p.getLevel() : 0);
        dto.setCurrentTier(p.getCurrentTier() != null ? p.getCurrentTier() : 0);
        dto.setCurrentTierPatched(p.getCurrentTierPatched());

        ValorantPlayerDto.Stats stats = new ValorantPlayerDto.Stats();
        stats.setKills(p.getKills() != null ? p.getKills() : 0);
        stats.setDeaths(p.getDeaths() != null ? p.getDeaths() : 0);
        stats.setAssists(p.getAssists() != null ? p.getAssists() : 0);
        stats.setScore(p.getScore() != null ? p.getScore() : 0);
        stats.setHeadshots(p.getHeadshots() != null ? p.getHeadshots() : 0);
        stats.setBodyshots(p.getBodyshots() != null ? p.getBodyshots() : 0);
        stats.setLegshots(p.getLegshots() != null ? p.getLegshots() : 0);
        dto.setStats(stats);

        dto.setDamageMade(p.getDamageMade() != null ? p.getDamageMade() : 0);
        dto.setDamageReceived(p.getDamageReceived() != null ? p.getDamageReceived() : 0);

        ValorantPlayerDto.AbilityCasts abilityCasts = new ValorantPlayerDto.AbilityCasts();
        abilityCasts.setXCast(p.getAbilityXCast() != null ? p.getAbilityXCast() : 0);
        abilityCasts.setECast(p.getAbilityECast() != null ? p.getAbilityECast() : 0);
        abilityCasts.setQCast(p.getAbilityQCast() != null ? p.getAbilityQCast() : 0);
        abilityCasts.setCCast(p.getAbilityCCast() != null ? p.getAbilityCCast() : 0);
        dto.setAbilityCasts(abilityCasts);

        if (p.getEconomySpentOverall() != null || p.getLoadoutValueOverall() != null) {
            ValorantPlayerDto.Economy economy = new ValorantPlayerDto.Economy();
            if (p.getEconomySpentOverall() != null) {
                ValorantPlayerDto.Spent spent = new ValorantPlayerDto.Spent();
                spent.setOverall(p.getEconomySpentOverall());
                spent.setAverage(p.getEconomySpentOverall());
                economy.setSpent(spent);
            }
            if (p.getLoadoutValueOverall() != null) {
                ValorantPlayerDto.LoadoutValue lv = new ValorantPlayerDto.LoadoutValue();
                lv.setOverall(p.getLoadoutValueOverall());
                lv.setAverage(p.getLoadoutValueOverall());
                economy.setLoadoutValue(lv);
            }
            dto.setEconomy(economy);
        }

        if (p.getAfkRounds() != null || p.getRoundsInSpawn() != null || p.getFriendlyFireIncoming() != null || p.getFriendlyFireOutgoing() != null) {
            ValorantPlayerDto.Behavior behavior = new ValorantPlayerDto.Behavior();
            behavior.setAfkRounds(p.getAfkRounds() != null ? p.getAfkRounds() : 0);
            behavior.setRoundsInSpawn(p.getRoundsInSpawn() != null ? p.getRoundsInSpawn() : 0);
            if (p.getFriendlyFireIncoming() != null || p.getFriendlyFireOutgoing() != null) {
                ValorantPlayerDto.FriendlyFire ff = new ValorantPlayerDto.FriendlyFire();
                ff.setIncoming(p.getFriendlyFireIncoming() != null ? p.getFriendlyFireIncoming() : 0);
                ff.setOutgoing(p.getFriendlyFireOutgoing() != null ? p.getFriendlyFireOutgoing() : 0);
                behavior.setFriendlyFire(ff);
            }
            dto.setBehavior(behavior);
        }

        return dto;
    }

    private List<ValorantMatchDetailDto.Round> toRoundsDto(ValorantMatch match) {
        List<ValorantMatchRound> rounds = match.getRounds();
        if (rounds == null) return Collections.emptyList();

        List<ValorantMatchDetailDto.Round> result = new ArrayList<>();
        for (ValorantMatchRound r : rounds) {
            result.add(toRoundDto(r));
        }
        return result;
    }

    private ValorantMatchDetailDto.Round toRoundDto(ValorantMatchRound r) {
        ValorantMatchDetailDto.Round round = new ValorantMatchDetailDto.Round();
        round.setWinningTeam(r.getWinningTeam());
        round.setEndType(r.getEndType());
        round.setBombPlanted(Boolean.TRUE.equals(r.getBombPlanted()));
        round.setBombDefused(Boolean.TRUE.equals(r.getBombDefused()));

        if (r.getPlantSite() != null || r.getPlantTimeInRound() != null || r.getPlantedByPuuid() != null
                || (r.getPlayerLocations() != null && r.getPlayerLocations().stream().anyMatch(pl -> "PLANT".equals(pl.getEventType())))) {
            ValorantMatchDetailDto.PlantEvents plant = new ValorantMatchDetailDto.PlantEvents();
            plant.setPlantSite(r.getPlantSite());
            plant.setPlantTimeInRound(r.getPlantTimeInRound() != null ? r.getPlantTimeInRound() : 0);
            if (r.getPlantedByPuuid() != null) {
                ValorantMatchDetailDto.PlayerRef ref = new ValorantMatchDetailDto.PlayerRef();
                ref.setPuuid(r.getPlantedByPuuid());
                plant.setPlantedBy(ref);
            }
            if (r.getPlantLocationX() != null || r.getPlantLocationY() != null) {
                ValorantMatchDetailDto.Location loc = new ValorantMatchDetailDto.Location();
                loc.setX(r.getPlantLocationX() != null ? r.getPlantLocationX() : 0);
                loc.setY(r.getPlantLocationY() != null ? r.getPlantLocationY() : 0);
                plant.setPlantLocation(loc);
            }
            if (r.getPlayerLocations() != null) {
                plant.setPlayerLocationsOnPlant(r.getPlayerLocations().stream()
                        .filter(pl -> "PLANT".equals(pl.getEventType()))
                        .map(this::toPlayerLocationDto)
                        .collect(Collectors.toList()));
            }
            round.setPlantEvents(plant);
        }

        if (r.getDefuseTimeInRound() != null || r.getDefusedByPuuid() != null
                || (r.getPlayerLocations() != null && r.getPlayerLocations().stream().anyMatch(pl -> "DEFUSE".equals(pl.getEventType())))) {
            ValorantMatchDetailDto.DefuseEvents defuse = new ValorantMatchDetailDto.DefuseEvents();
            defuse.setDefuseTimeInRound(r.getDefuseTimeInRound() != null ? r.getDefuseTimeInRound() : 0);
            if (r.getDefusedByPuuid() != null) {
                ValorantMatchDetailDto.PlayerRef ref = new ValorantMatchDetailDto.PlayerRef();
                ref.setPuuid(r.getDefusedByPuuid());
                defuse.setDefusedBy(ref);
            }
            if (r.getDefuseLocationX() != null || r.getDefuseLocationY() != null) {
                ValorantMatchDetailDto.Location loc = new ValorantMatchDetailDto.Location();
                loc.setX(r.getDefuseLocationX() != null ? r.getDefuseLocationX() : 0);
                loc.setY(r.getDefuseLocationY() != null ? r.getDefuseLocationY() : 0);
                defuse.setDefuseLocation(loc);
            }
            if (r.getPlayerLocations() != null) {
                defuse.setPlayerLocationsOnDefuse(r.getPlayerLocations().stream()
                        .filter(pl -> "DEFUSE".equals(pl.getEventType()))
                        .map(this::toPlayerLocationDto)
                        .collect(Collectors.toList()));
            }
            round.setDefuseEvents(defuse);
        }

        if (r.getPlayerStats() != null && !r.getPlayerStats().isEmpty()) {
            round.setPlayerStats(r.getPlayerStats().stream()
                    .map(this::toRoundPlayerStatDto)
                    .collect(Collectors.toList()));
        }

        return round;
    }

    private ValorantMatchDetailDto.RoundPlayerStat toRoundPlayerStatDto(ValorantMatchRoundPlayer ps) {
        ValorantMatchDetailDto.RoundPlayerStat stat = new ValorantMatchDetailDto.RoundPlayerStat();
        stat.setPlayerPuuid(ps.getPlayerPuuid());
        stat.setPlayerDisplayName(ps.getPlayerDisplayName());
        stat.setPlayerTeam(ps.getPlayerTeam());
        stat.setDamage(ps.getDamage() != null ? ps.getDamage() : 0);
        stat.setHeadshots(ps.getHeadshots() != null ? ps.getHeadshots() : 0);
        stat.setBodyshots(ps.getBodyshots() != null ? ps.getBodyshots() : 0);
        stat.setLegshots(ps.getLegshots() != null ? ps.getLegshots() : 0);
        stat.setKills(ps.getKills() != null ? ps.getKills() : 0);
        stat.setScore(ps.getScore() != null ? ps.getScore() : 0);
        stat.setWasAfk(Boolean.TRUE.equals(ps.getWasAfk()));
        stat.setWasPenalized(Boolean.TRUE.equals(ps.getWasPenalized()));
        stat.setStayedInSpawn(Boolean.TRUE.equals(ps.getStayedInSpawn()));

        if (ps.getDamageEvents() != null && !ps.getDamageEvents().isEmpty()) {
            stat.setDamageEvents(ps.getDamageEvents().stream()
                    .map(this::toDamageEventDto)
                    .collect(Collectors.toList()));
        }

        if (ps.getAbilityXCasts() != null || ps.getAbilityECasts() != null || ps.getAbilityQCasts() != null || ps.getAbilityCCasts() != null) {
            ValorantMatchDetailDto.RoundAbilityCasts ac = new ValorantMatchDetailDto.RoundAbilityCasts();
            ac.setXCasts(ps.getAbilityXCasts());
            ac.setECasts(ps.getAbilityECasts());
            ac.setQCasts(ps.getAbilityQCasts());
            ac.setCCasts(ps.getAbilityCCasts());
            stat.setAbilityCasts(ac);
        }

        if (ps.getLoadoutValue() != null || ps.getEconomyRemaining() != null || ps.getEconomySpent() != null) {
            ValorantMatchDetailDto.RoundEconomy economy = new ValorantMatchDetailDto.RoundEconomy();
            economy.setLoadoutValue(ps.getLoadoutValue() != null ? ps.getLoadoutValue() : 0);
            economy.setRemaining(ps.getEconomyRemaining() != null ? ps.getEconomyRemaining() : 0);
            economy.setSpent(ps.getEconomySpent() != null ? ps.getEconomySpent() : 0);
            stat.setEconomy(economy);
        }

        return stat;
    }

    private ValorantMatchDetailDto.PlayerLocation toPlayerLocationDto(ValorantRoundPlayerLocation pl) {
        ValorantMatchDetailDto.PlayerLocation loc = new ValorantMatchDetailDto.PlayerLocation();
        loc.setPlayerPuuid(pl.getPlayerPuuid());
        loc.setPlayerDisplayName(pl.getPlayerDisplayName());
        loc.setPlayerTeam(pl.getPlayerTeam());
        if (pl.getLocationX() != null || pl.getLocationY() != null) {
            ValorantMatchDetailDto.Location location = new ValorantMatchDetailDto.Location();
            location.setX(pl.getLocationX() != null ? pl.getLocationX() : 0);
            location.setY(pl.getLocationY() != null ? pl.getLocationY() : 0);
            loc.setLocation(location);
        }
        loc.setViewRadians(pl.getViewRadians() != null ? pl.getViewRadians() : 0.0);
        return loc;
    }

    private ValorantMatchDetailDto.DamageEvent toDamageEventDto(ValorantRoundPlayerDamageEvent de) {
        ValorantMatchDetailDto.DamageEvent event = new ValorantMatchDetailDto.DamageEvent();
        event.setReceiverPuuid(de.getReceiverPuuid());
        event.setReceiverDisplayName(de.getReceiverDisplayName());
        event.setReceiverTeam(de.getReceiverTeam());
        event.setBodyshots(de.getBodyshots() != null ? de.getBodyshots() : 0);
        event.setHeadshots(de.getHeadshots() != null ? de.getHeadshots() : 0);
        event.setLegshots(de.getLegshots() != null ? de.getLegshots() : 0);
        event.setDamage(de.getDamage() != null ? de.getDamage() : 0);
        return event;
    }

    private List<ValorantMatchDetailDto.KillEvent> toKillEventsDto(ValorantMatch match) {
        List<ValorantKillEvent> events = match.getKillEvents();
        if (events == null) return Collections.emptyList();

        return events.stream()
                .map(this::toKillEventDto)
                .collect(Collectors.toList());
    }

    private ValorantMatchDetailDto.KillEvent toKillEventDto(ValorantKillEvent ke) {
        ValorantMatchDetailDto.KillEvent event = new ValorantMatchDetailDto.KillEvent();
        event.setRound(ke.getRoundNumber());
        event.setKillTimeInRound(ke.getKillTimeInRound() != null ? ke.getKillTimeInRound() : 0);
        event.setKillTimeInMatch(ke.getKillTimeInMatch() != null ? ke.getKillTimeInMatch() : 0);
        event.setKillerPuuid(ke.getKillerPuuid());
        event.setKillerDisplayName(ke.getKillerDisplayName());
        event.setKillerTeam(ke.getKillerTeam());
        event.setVictimPuuid(ke.getVictimPuuid());
        event.setVictimDisplayName(ke.getVictimDisplayName());
        event.setVictimTeam(ke.getVictimTeam());
        if (ke.getVictimDeathX() != null || ke.getVictimDeathY() != null) {
            ValorantMatchDetailDto.Location loc = new ValorantMatchDetailDto.Location();
            loc.setX(ke.getVictimDeathX() != null ? ke.getVictimDeathX() : 0);
            loc.setY(ke.getVictimDeathY() != null ? ke.getVictimDeathY() : 0);
            event.setVictimDeathLocation(loc);
        }
        event.setDamageWeaponId(ke.getDamageWeaponId());
        event.setDamageWeaponName(ke.getDamageWeaponName());
        event.setSecondaryFireMode(Boolean.TRUE.equals(ke.getSecondaryFireMode()));

        if (ke.getAssistants() != null && !ke.getAssistants().isEmpty()) {
            event.setAssistants(ke.getAssistants().stream()
                    .map(a -> {
                        ValorantMatchDetailDto.Assistant ass = new ValorantMatchDetailDto.Assistant();
                        ass.setAssistantPuuid(a.getAssistantPuuid());
                        ass.setAssistantDisplayName(a.getAssistantDisplayName());
                        ass.setAssistantTeam(a.getAssistantTeam());
                        return ass;
                    })
                    .collect(Collectors.toList()));
        }

        return event;
    }
}
