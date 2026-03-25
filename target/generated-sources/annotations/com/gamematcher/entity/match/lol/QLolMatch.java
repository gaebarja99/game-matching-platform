package com.gamematcher.entity.match.lol;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLolMatch is a Querydsl query type for LolMatch
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLolMatch extends EntityPathBase<LolMatch> {

    private static final long serialVersionUID = -482233222L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLolMatch lolMatch = new QLolMatch("lolMatch");

    public final DateTimePath<java.time.LocalDateTime> apiCachedAt = createDateTime("apiCachedAt", java.time.LocalDateTime.class);

    public final StringPath dataVersion = createString("dataVersion");

    public final StringPath endOfGameResult = createString("endOfGameResult");

    public final NumberPath<Long> gameCreation = createNumber("gameCreation", Long.class);

    public final NumberPath<Long> gameDuration = createNumber("gameDuration", Long.class);

    public final NumberPath<Long> gameEndTimestamp = createNumber("gameEndTimestamp", Long.class);

    public final NumberPath<Long> gameId = createNumber("gameId", Long.class);

    public final StringPath gameMode = createString("gameMode");

    public final StringPath gameName = createString("gameName");

    public final NumberPath<Long> gameStartTimestamp = createNumber("gameStartTimestamp", Long.class);

    public final StringPath gameType = createString("gameType");

    public final StringPath gameVersion = createString("gameVersion");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> mapId = createNumber("mapId", Integer.class);

    public final StringPath matchId = createString("matchId");

    public final ListPath<LolMatchParticipant, QLolMatchParticipant> participants = this.<LolMatchParticipant, QLolMatchParticipant>createList("participants", LolMatchParticipant.class, QLolMatchParticipant.class, PathInits.DIRECT2);

    public final StringPath platformId = createString("platformId");

    public final NumberPath<Integer> queueId = createNumber("queueId", Integer.class);

    public final ListPath<LolMatchTeam, QLolMatchTeam> teams = this.<LolMatchTeam, QLolMatchTeam>createList("teams", LolMatchTeam.class, QLolMatchTeam.class, PathInits.DIRECT2);

    public final QLolMatchTimeline timeline;

    public final StringPath tournamentCode = createString("tournamentCode");

    public QLolMatch(String variable) {
        this(LolMatch.class, forVariable(variable), INITS);
    }

    public QLolMatch(Path<? extends LolMatch> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLolMatch(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLolMatch(PathMetadata metadata, PathInits inits) {
        this(LolMatch.class, metadata, inits);
    }

    public QLolMatch(Class<? extends LolMatch> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.timeline = inits.isInitialized("timeline") ? new QLolMatchTimeline(forProperty("timeline"), inits.get("timeline")) : null;
    }

}

