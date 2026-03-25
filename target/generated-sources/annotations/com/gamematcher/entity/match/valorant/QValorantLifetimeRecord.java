package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QValorantLifetimeRecord is a Querydsl query type for ValorantLifetimeRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantLifetimeRecord extends EntityPathBase<ValorantLifetimeRecord> {

    private static final long serialVersionUID = 1541265103L;

    public static final QValorantLifetimeRecord valorantLifetimeRecord = new QValorantLifetimeRecord("valorantLifetimeRecord");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Integer> blueRounds = createNumber("blueRounds", Integer.class);

    public final StringPath characterId = createString("characterId");

    public final StringPath characterName = createString("characterName");

    public final StringPath cluster = createString("cluster");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> damageMade = createNumber("damageMade", Integer.class);

    public final NumberPath<Integer> damageReceived = createNumber("damageReceived", Integer.class);

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final NumberPath<Integer> level = createNumber("level", Integer.class);

    public final StringPath mapId = createString("mapId");

    public final StringPath mapName = createString("mapName");

    public final StringPath matchId = createString("matchId");

    public final StringPath mode = createString("mode");

    public final StringPath name = createString("name");

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Integer> redRounds = createNumber("redRounds", Integer.class);

    public final StringPath region = createString("region");

    public final StringPath riotMatchId = createString("riotMatchId");

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final StringPath seasonId = createString("seasonId");

    public final StringPath seasonShort = createString("seasonShort");

    public final NumberPath<Integer> shotsBody = createNumber("shotsBody", Integer.class);

    public final NumberPath<Integer> shotsHead = createNumber("shotsHead", Integer.class);

    public final NumberPath<Integer> shotsLeg = createNumber("shotsLeg", Integer.class);

    public final StringPath startedAt = createString("startedAt");

    public final StringPath tag = createString("tag");

    public final StringPath team = createString("team");

    public final NumberPath<Integer> tier = createNumber("tier", Integer.class);

    public final StringPath version = createString("version");

    public QValorantLifetimeRecord(String variable) {
        super(ValorantLifetimeRecord.class, forVariable(variable));
    }

    public QValorantLifetimeRecord(Path<? extends ValorantLifetimeRecord> path) {
        super(path.getType(), path.getMetadata());
    }

    public QValorantLifetimeRecord(PathMetadata metadata) {
        super(ValorantLifetimeRecord.class, metadata);
    }

}

