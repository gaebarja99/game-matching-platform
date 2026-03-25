package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMatchSessionMember is a Querydsl query type for MatchSessionMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchSessionMember extends EntityPathBase<MatchSessionMember> {

    private static final long serialVersionUID = -1933024005L;

    public static final QMatchSessionMember matchSessionMember = new QMatchSessionMember("matchSessionMember");

    public final StringPath assignedLane = createString("assignedLane");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> sessionId = createNumber("sessionId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QMatchSessionMember(String variable) {
        super(MatchSessionMember.class, forVariable(variable));
    }

    public QMatchSessionMember(Path<? extends MatchSessionMember> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMatchSessionMember(PathMetadata metadata) {
        super(MatchSessionMember.class, metadata);
    }

}

