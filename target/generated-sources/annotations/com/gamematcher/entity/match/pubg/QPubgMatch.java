package com.gamematcher.entity.match.pubg;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPubgMatch is a Querydsl query type for PubgMatch
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPubgMatch extends EntityPathBase<PubgMatch> {

    private static final long serialVersionUID = -2094354448L;

    public static final QPubgMatch pubgMatch = new QPubgMatch("pubgMatch");

    public final DateTimePath<java.time.LocalDateTime> apiCachedAt = createDateTime("apiCachedAt", java.time.LocalDateTime.class);

    public final StringPath createdAt = createString("createdAt");

    public final NumberPath<Integer> duration = createNumber("duration", Integer.class);

    public final StringPath gameMode = createString("gameMode");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isCustomMatch = createBoolean("isCustomMatch");

    public final StringPath mapName = createString("mapName");

    public final StringPath matchId = createString("matchId");

    public final StringPath matchType = createString("matchType");

    public final ListPath<PubgMatchParticipant, QPubgMatchParticipant> participants = this.<PubgMatchParticipant, QPubgMatchParticipant>createList("participants", PubgMatchParticipant.class, QPubgMatchParticipant.class, PathInits.DIRECT2);

    public final StringPath seasonState = createString("seasonState");

    public final StringPath shardId = createString("shardId");

    public final StringPath titleId = createString("titleId");

    public QPubgMatch(String variable) {
        super(PubgMatch.class, forVariable(variable));
    }

    public QPubgMatch(Path<? extends PubgMatch> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPubgMatch(PathMetadata metadata) {
        super(PubgMatch.class, metadata);
    }

}

