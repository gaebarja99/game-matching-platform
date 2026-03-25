package com.gamematcher.entity.match.lol;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLolMatchParticipant is a Querydsl query type for LolMatchParticipant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLolMatchParticipant extends EntityPathBase<LolMatchParticipant> {

    private static final long serialVersionUID = 1580610137L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLolMatchParticipant lolMatchParticipant = new QLolMatchParticipant("lolMatchParticipant");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Integer> championId = createNumber("championId", Integer.class);

    public final StringPath championName = createString("championName");

    public final NumberPath<Integer> champLevel = createNumber("champLevel", Integer.class);

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final NumberPath<Integer> goldEarned = createNumber("goldEarned", Integer.class);

    public final NumberPath<Integer> goldSpent = createNumber("goldSpent", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath individualPosition = createString("individualPosition");

    public final NumberPath<Integer> item0 = createNumber("item0", Integer.class);

    public final NumberPath<Integer> item1 = createNumber("item1", Integer.class);

    public final NumberPath<Integer> item2 = createNumber("item2", Integer.class);

    public final NumberPath<Integer> item3 = createNumber("item3", Integer.class);

    public final NumberPath<Integer> item4 = createNumber("item4", Integer.class);

    public final NumberPath<Integer> item5 = createNumber("item5", Integer.class);

    public final NumberPath<Integer> item6 = createNumber("item6", Integer.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final QLolMatch match;

    public final NumberPath<Integer> neutralMinionsKilled = createNumber("neutralMinionsKilled", Integer.class);

    public final NumberPath<Integer> participantId = createNumber("participantId", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final StringPath riotIdGameName = createString("riotIdGameName");

    public final StringPath riotIdTagline = createString("riotIdTagline");

    public final NumberPath<Integer> summoner1Id = createNumber("summoner1Id", Integer.class);

    public final NumberPath<Integer> summoner2Id = createNumber("summoner2Id", Integer.class);

    public final StringPath summonerId = createString("summonerId");

    public final NumberPath<Integer> teamId = createNumber("teamId", Integer.class);

    public final StringPath teamPosition = createString("teamPosition");

    public final NumberPath<Integer> totalDamageDealtToChampions = createNumber("totalDamageDealtToChampions", Integer.class);

    public final NumberPath<Integer> totalDamageTaken = createNumber("totalDamageTaken", Integer.class);

    public final NumberPath<Integer> totalMinionsKilled = createNumber("totalMinionsKilled", Integer.class);

    public final NumberPath<Integer> visionScore = createNumber("visionScore", Integer.class);

    public final BooleanPath win = createBoolean("win");

    public QLolMatchParticipant(String variable) {
        this(LolMatchParticipant.class, forVariable(variable), INITS);
    }

    public QLolMatchParticipant(Path<? extends LolMatchParticipant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLolMatchParticipant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLolMatchParticipant(PathMetadata metadata, PathInits inits) {
        this(LolMatchParticipant.class, metadata, inits);
    }

    public QLolMatchParticipant(Class<? extends LolMatchParticipant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QLolMatch(forProperty("match"), inits.get("match")) : null;
    }

}

