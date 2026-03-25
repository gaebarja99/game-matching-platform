package com.gamematcher.entity.match.lol;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLolMatchTimeline is a Querydsl query type for LolMatchTimeline
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLolMatchTimeline extends EntityPathBase<LolMatchTimeline> {

    private static final long serialVersionUID = 1627247323L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLolMatchTimeline lolMatchTimeline = new QLolMatchTimeline("lolMatchTimeline");

    public final StringPath endOfGameResult = createString("endOfGameResult");

    public final NumberPath<Integer> frameInterval = createNumber("frameInterval", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QLolMatch match;

    public final StringPath timelineInfo = createString("timelineInfo");

    public QLolMatchTimeline(String variable) {
        this(LolMatchTimeline.class, forVariable(variable), INITS);
    }

    public QLolMatchTimeline(Path<? extends LolMatchTimeline> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLolMatchTimeline(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLolMatchTimeline(PathMetadata metadata, PathInits inits) {
        this(LolMatchTimeline.class, metadata, inits);
    }

    public QLolMatchTimeline(Class<? extends LolMatchTimeline> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QLolMatch(forProperty("match"), inits.get("match")) : null;
    }

}

