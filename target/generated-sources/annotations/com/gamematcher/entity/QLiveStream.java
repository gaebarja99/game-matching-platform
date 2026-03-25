package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLiveStream is a Querydsl query type for LiveStream
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLiveStream extends EntityPathBase<LiveStream> {

    private static final long serialVersionUID = -842952324L;

    public static final QLiveStream liveStream = new QLiveStream("liveStream");

    public final BooleanPath chatFrozen = createBoolean("chatFrozen");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> endedAt = createDateTime("endedAt", java.time.LocalDateTime.class);

    public final StringPath externalUrl = createString("externalUrl");

    public final EnumPath<com.gamematcher.constant.GameList> game = createEnum("game", com.gamematcher.constant.GameList.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> minTtsPang = createNumber("minTtsPang", Integer.class);

    public final NumberPath<Integer> minVideoPang = createNumber("minVideoPang", Integer.class);

    public final StringPath playbackUrl = createString("playbackUrl");

    public final DateTimePath<java.time.LocalDateTime> startedAt = createDateTime("startedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.gamematcher.constant.StreamStatus> status = createEnum("status", com.gamematcher.constant.StreamStatus.class);

    public final StringPath streamKey = createString("streamKey");

    public final StringPath streamNotice = createString("streamNotice");

    public final BooleanPath streamNoticeVisible = createBoolean("streamNoticeVisible");

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QLiveStream(String variable) {
        super(LiveStream.class, forVariable(variable));
    }

    public QLiveStream(Path<? extends LiveStream> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLiveStream(PathMetadata metadata) {
        super(LiveStream.class, metadata);
    }

}

