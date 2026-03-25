package com.gamematcher.entity.ai.evaluation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchRecord is a Querydsl query type for MatchRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchRecord extends EntityPathBase<MatchRecord> {

    private static final long serialVersionUID = 1220658010L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchRecord matchRecord = new QMatchRecord("matchRecord");

    public final QGame game;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath matchId = createString("matchId");

    public final DateTimePath<java.time.LocalDateTime> playedAt = createDateTime("playedAt", java.time.LocalDateTime.class);

    public final MapPath<String, Object, SimplePath<Object>> rawData = this.<String, Object, SimplePath<Object>>createMap("rawData", String.class, Object.class, SimplePath.class);

    public final EnumPath<com.gamematcher.constant.ai.evaluation.MatchResult> result = createEnum("result", com.gamematcher.constant.ai.evaluation.MatchResult.class);

    public QMatchRecord(String variable) {
        this(MatchRecord.class, forVariable(variable), INITS);
    }

    public QMatchRecord(Path<? extends MatchRecord> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchRecord(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchRecord(PathMetadata metadata, PathInits inits) {
        this(MatchRecord.class, metadata, inits);
    }

    public QMatchRecord(Class<? extends MatchRecord> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.game = inits.isInitialized("game") ? new QGame(forProperty("game")) : null;
    }

}

