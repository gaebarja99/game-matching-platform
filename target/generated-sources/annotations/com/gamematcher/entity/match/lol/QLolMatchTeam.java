package com.gamematcher.entity.match.lol;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLolMatchTeam is a Querydsl query type for LolMatchTeam
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLolMatchTeam extends EntityPathBase<LolMatchTeam> {

    private static final long serialVersionUID = 1244044791L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLolMatchTeam lolMatchTeam = new QLolMatchTeam("lolMatchTeam");

    public final StringPath bans = createString("bans");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QLolMatch match;

    public final StringPath objectives = createString("objectives");

    public final NumberPath<Integer> teamId = createNumber("teamId", Integer.class);

    public final BooleanPath win = createBoolean("win");

    public QLolMatchTeam(String variable) {
        this(LolMatchTeam.class, forVariable(variable), INITS);
    }

    public QLolMatchTeam(Path<? extends LolMatchTeam> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLolMatchTeam(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLolMatchTeam(PathMetadata metadata, PathInits inits) {
        this(LolMatchTeam.class, metadata, inits);
    }

    public QLolMatchTeam(Class<? extends LolMatchTeam> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QLolMatch(forProperty("match"), inits.get("match")) : null;
    }

}

