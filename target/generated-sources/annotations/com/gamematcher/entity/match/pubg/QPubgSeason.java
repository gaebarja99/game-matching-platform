package com.gamematcher.entity.match.pubg;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPubgSeason is a Querydsl query type for PubgSeason
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPubgSeason extends EntityPathBase<PubgSeason> {

    private static final long serialVersionUID = -325559784L;

    public static final QPubgSeason pubgSeason = new QPubgSeason("pubgSeason");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isCurrentSeason = createBoolean("isCurrentSeason");

    public final BooleanPath isOffseason = createBoolean("isOffseason");

    public final StringPath platform = createString("platform");

    public final StringPath seasonId = createString("seasonId");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QPubgSeason(String variable) {
        super(PubgSeason.class, forVariable(variable));
    }

    public QPubgSeason(Path<? extends PubgSeason> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPubgSeason(PathMetadata metadata) {
        super(PubgSeason.class, metadata);
    }

}

