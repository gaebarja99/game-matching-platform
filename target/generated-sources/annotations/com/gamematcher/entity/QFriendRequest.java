package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QFriendRequest is a Querydsl query type for FriendRequest
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFriendRequest extends EntityPathBase<FriendRequest> {

    private static final long serialVersionUID = -1198201311L;

    public static final QFriendRequest friendRequest = new QFriendRequest("friendRequest");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> fromUserId = createNumber("fromUserId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<FriendRequest.FriendRequestStatus> status = createEnum("status", FriendRequest.FriendRequestStatus.class);

    public final NumberPath<Long> toUserId = createNumber("toUserId", Long.class);

    public QFriendRequest(String variable) {
        super(FriendRequest.class, forVariable(variable));
    }

    public QFriendRequest(Path<? extends FriendRequest> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFriendRequest(PathMetadata metadata) {
        super(FriendRequest.class, metadata);
    }

}

