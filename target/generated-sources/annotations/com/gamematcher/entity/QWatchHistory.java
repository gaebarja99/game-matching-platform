package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QWatchHistory is a Querydsl query type for WatchHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWatchHistory extends EntityPathBase<WatchHistory> {

    private static final long serialVersionUID = 1599916725L;

    public static final QWatchHistory watchHistory = new QWatchHistory("watchHistory");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> streamId = createNumber("streamId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> viewerId = createNumber("viewerId", Long.class);

    public final NumberPath<Long> watchSeconds = createNumber("watchSeconds", Long.class);

    public QWatchHistory(String variable) {
        super(WatchHistory.class, forVariable(variable));
    }

    public QWatchHistory(Path<? extends WatchHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWatchHistory(PathMetadata metadata) {
        super(WatchHistory.class, metadata);
    }

}

