package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserPushToken is a Querydsl query type for UserPushToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserPushToken extends EntityPathBase<UserPushToken> {

    private static final long serialVersionUID = 691779460L;

    public static final QUserPushToken userPushToken = new QUserPushToken("userPushToken");

    public final StringPath fcmToken = createString("fcmToken");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath platform = createString("platform");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserPushToken(String variable) {
        super(UserPushToken.class, forVariable(variable));
    }

    public QUserPushToken(Path<? extends UserPushToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserPushToken(PathMetadata metadata) {
        super(UserPushToken.class, metadata);
    }

}

