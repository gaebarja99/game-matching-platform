package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 1875387835L;

    public static final QUser user = new QUser("user");

    public final DateTimePath<java.time.LocalDateTime> adFreeUntil = createDateTime("adFreeUntil", java.time.LocalDateTime.class);

    public final StringPath authToken = createString("authToken");

    public final StringPath bio = createString("bio");

    public final DateTimePath<java.time.LocalDateTime> chatMutedUntil = createDateTime("chatMutedUntil", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastLoginAt = createDateTime("lastLoginAt", java.time.LocalDateTime.class);

    public final StringPath loginId = createString("loginId");

    public final NumberPath<Long> mileage = createNumber("mileage", Long.class);

    public final StringPath nickname = createString("nickname");

    public final NumberPath<Long> pangBalance = createNumber("pangBalance", Long.class);

    public final StringPath password = createString("password");

    public final StringPath phone = createString("phone");

    public final NumberPath<Integer> profanityStrikeCount = createNumber("profanityStrikeCount", Integer.class);

    public final StringPath profileImageUrl = createString("profileImageUrl");

    public final EnumPath<com.gamematcher.constant.Provider> provider = createEnum("provider", com.gamematcher.constant.Provider.class);

    public final StringPath providerSubject = createString("providerSubject");

    public final EnumPath<com.gamematcher.constant.Role> role = createEnum("role", com.gamematcher.constant.Role.class);

    public final EnumPath<com.gamematcher.constant.UserStatus> status = createEnum("status", com.gamematcher.constant.UserStatus.class);

    public final EnumPath<com.gamematcher.constant.StreamerTier> streamerTier = createEnum("streamerTier", com.gamematcher.constant.StreamerTier.class);

    public final NumberPath<Long> totalExperienceTenths = createNumber("totalExperienceTenths", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath username = createString("username");

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

