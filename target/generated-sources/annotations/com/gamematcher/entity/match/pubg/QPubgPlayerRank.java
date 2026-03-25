package com.gamematcher.entity.match.pubg;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPubgPlayerRank is a Querydsl query type for PubgPlayerRank
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPubgPlayerRank extends EntityPathBase<PubgPlayerRank> {

    private static final long serialVersionUID = 870600546L;

    public static final QPubgPlayerRank pubgPlayerRank = new QPubgPlayerRank("pubgPlayerRank");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Double> avgKill = createNumber("avgKill", Double.class);

    public final NumberPath<Double> avgRank = createNumber("avgRank", Double.class);

    public final NumberPath<Double> avgSurvivalTime = createNumber("avgSurvivalTime", Double.class);

    public final NumberPath<Integer> bestRankPoint = createNumber("bestRankPoint", Integer.class);

    public final StringPath bestSubTier = createString("bestSubTier");

    public final StringPath bestTier = createString("bestTier");

    public final NumberPath<Integer> boosts = createNumber("boosts", Integer.class);

    public final NumberPath<Integer> currentRankPoint = createNumber("currentRankPoint", Integer.class);

    public final StringPath currentTier = createString("currentTier");

    public final NumberPath<Double> damageDealt = createNumber("damageDealt", Double.class);

    public final NumberPath<Integer> dbnos = createNumber("dbnos", Integer.class);

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final StringPath gameMode = createString("gameMode");

    public final NumberPath<Double> headshotKillRatio = createNumber("headshotKillRatio", Double.class);

    public final NumberPath<Integer> headshotKills = createNumber("headshotKills", Integer.class);

    public final NumberPath<Integer> heals = createNumber("heals", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final NumberPath<Integer> killStreak = createNumber("killStreak", Integer.class);

    public final NumberPath<Double> longestKill = createNumber("longestKill", Double.class);

    public final StringPath platform = createString("platform");

    public final StringPath playerId = createString("playerId");

    public final NumberPath<Integer> playTime = createNumber("playTime", Integer.class);

    public final NumberPath<Double> reviveRatio = createNumber("reviveRatio", Double.class);

    public final NumberPath<Integer> revives = createNumber("revives", Integer.class);

    public final NumberPath<Integer> roundMostKills = createNumber("roundMostKills", Integer.class);

    public final NumberPath<Integer> roundsPlayed = createNumber("roundsPlayed", Integer.class);

    public final StringPath seasonId = createString("seasonId");

    public final StringPath subTier = createString("subTier");

    public final NumberPath<Integer> teamKills = createNumber("teamKills", Integer.class);

    public final NumberPath<Double> top10Ratio = createNumber("top10Ratio", Double.class);

    public final NumberPath<Integer> weaponsAcquired = createNumber("weaponsAcquired", Integer.class);

    public final NumberPath<Double> winRatio = createNumber("winRatio", Double.class);

    public final NumberPath<Integer> wins = createNumber("wins", Integer.class);

    public QPubgPlayerRank(String variable) {
        super(PubgPlayerRank.class, forVariable(variable));
    }

    public QPubgPlayerRank(Path<? extends PubgPlayerRank> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPubgPlayerRank(PathMetadata metadata) {
        super(PubgPlayerRank.class, metadata);
    }

}

