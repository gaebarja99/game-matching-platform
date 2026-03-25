package com.gamematcher.entity.match;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTftMatch is a Querydsl query type for TftMatch
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTftMatch extends EntityPathBase<TftMatch> {

    private static final long serialVersionUID = 509636252L;

    public static final QTftMatch tftMatch = new QTftMatch("tftMatch");

    public final DateTimePath<java.time.LocalDateTime> apiCachedAt = createDateTime("apiCachedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> gameCreation = createNumber("gameCreation", Long.class);

    public final NumberPath<Integer> gameDuration = createNumber("gameDuration", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> level = createNumber("level", Integer.class);

    public final StringPath matchId = createString("matchId");

    public final NumberPath<Integer> placement = createNumber("placement", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Integer> totalPlayers = createNumber("totalPlayers", Integer.class);

    public final StringPath traits = createString("traits");

    public final StringPath units = createString("units");

    public QTftMatch(String variable) {
        super(TftMatch.class, forVariable(variable));
    }

    public QTftMatch(Path<? extends TftMatch> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTftMatch(PathMetadata metadata) {
        super(TftMatch.class, metadata);
    }

}

