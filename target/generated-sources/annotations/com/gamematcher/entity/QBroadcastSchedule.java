package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBroadcastSchedule is a Querydsl query type for BroadcastSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBroadcastSchedule extends EntityPathBase<BroadcastSchedule> {

    private static final long serialVersionUID = 2060647048L;

    public static final QBroadcastSchedule broadcastSchedule = new QBroadcastSchedule("broadcastSchedule");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath link = createString("link");

    public final DateTimePath<java.time.LocalDateTime> scheduleAt = createDateTime("scheduleAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final StringPath title = createString("title");

    public QBroadcastSchedule(String variable) {
        super(BroadcastSchedule.class, forVariable(variable));
    }

    public QBroadcastSchedule(Path<? extends BroadcastSchedule> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBroadcastSchedule(PathMetadata metadata) {
        super(BroadcastSchedule.class, metadata);
    }

}

