package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantMatch is a Querydsl query type for ValorantMatch
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantMatch extends EntityPathBase<ValorantMatch> {

    private static final long serialVersionUID = 1867415760L;

    public static final QValorantMatch valorantMatch = new QValorantMatch("valorantMatch");

    public final DateTimePath<java.time.LocalDateTime> apiCachedAt = createDateTime("apiCachedAt", java.time.LocalDateTime.class);

    public final BooleanPath blueHasWon = createBoolean("blueHasWon");

    public final NumberPath<Integer> blueRoundsWon = createNumber("blueRoundsWon", Integer.class);

    public final StringPath cluster = createString("cluster");

    public final NumberPath<Integer> gameLength = createNumber("gameLength", Integer.class);

    public final NumberPath<Long> gameStart = createNumber("gameStart", Long.class);

    public final StringPath gameStartPatched = createString("gameStartPatched");

    public final StringPath gameVersion = createString("gameVersion");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<ValorantKillEvent, QValorantKillEvent> killEvents = this.<ValorantKillEvent, QValorantKillEvent>createList("killEvents", ValorantKillEvent.class, QValorantKillEvent.class, PathInits.DIRECT2);

    public final StringPath map = createString("map");

    public final StringPath matchId = createString("matchId");

    public final StringPath mode = createString("mode");

    public final StringPath modeId = createString("modeId");

    public final StringPath platform = createString("platform");

    public final ListPath<ValorantMatchPlayer, QValorantMatchPlayer> players = this.<ValorantMatchPlayer, QValorantMatchPlayer>createList("players", ValorantMatchPlayer.class, QValorantMatchPlayer.class, PathInits.DIRECT2);

    public final StringPath premierMatchupId = createString("premierMatchupId");

    public final StringPath premierTournamentId = createString("premierTournamentId");

    public final StringPath queue = createString("queue");

    public final BooleanPath redHasWon = createBoolean("redHasWon");

    public final NumberPath<Integer> redRoundsWon = createNumber("redRoundsWon", Integer.class);

    public final StringPath region = createString("region");

    public final ListPath<ValorantMatchRound, QValorantMatchRound> rounds = this.<ValorantMatchRound, QValorantMatchRound>createList("rounds", ValorantMatchRound.class, QValorantMatchRound.class, PathInits.DIRECT2);

    public final NumberPath<Integer> roundsPlayed = createNumber("roundsPlayed", Integer.class);

    public final StringPath seasonId = createString("seasonId");

    public QValorantMatch(String variable) {
        super(ValorantMatch.class, forVariable(variable));
    }

    public QValorantMatch(Path<? extends ValorantMatch> path) {
        super(path.getType(), path.getMetadata());
    }

    public QValorantMatch(PathMetadata metadata) {
        super(ValorantMatch.class, metadata);
    }

}

