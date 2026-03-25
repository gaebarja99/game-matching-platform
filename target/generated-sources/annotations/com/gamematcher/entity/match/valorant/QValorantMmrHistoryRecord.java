package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QValorantMmrHistoryRecord is a Querydsl query type for ValorantMmrHistoryRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantMmrHistoryRecord extends EntityPathBase<ValorantMmrHistoryRecord> {

    private static final long serialVersionUID = -1411891096L;

    public static final QValorantMmrHistoryRecord valorantMmrHistoryRecord = new QValorantMmrHistoryRecord("valorantMmrHistoryRecord");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> currentTier = createNumber("currentTier", Integer.class);

    public final StringPath currentTierPatched = createString("currentTierPatched");

    public final StringPath date = createString("date");

    public final NumberPath<Long> dateRaw = createNumber("dateRaw", Long.class);

    public final NumberPath<Integer> elo = createNumber("elo", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageLarge = createString("imageLarge");

    public final StringPath imageSmall = createString("imageSmall");

    public final StringPath mapId = createString("mapId");

    public final StringPath mapName = createString("mapName");

    public final StringPath matchId = createString("matchId");

    public final NumberPath<Integer> mmrChangeToLastGame = createNumber("mmrChangeToLastGame", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Integer> rankingInTier = createNumber("rankingInTier", Integer.class);

    public final StringPath seasonId = createString("seasonId");

    public QValorantMmrHistoryRecord(String variable) {
        super(ValorantMmrHistoryRecord.class, forVariable(variable));
    }

    public QValorantMmrHistoryRecord(Path<? extends ValorantMmrHistoryRecord> path) {
        super(path.getType(), path.getMetadata());
    }

    public QValorantMmrHistoryRecord(PathMetadata metadata) {
        super(ValorantMmrHistoryRecord.class, metadata);
    }

}

