package com.gamematcher.entity.account;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSteamAccount is a Querydsl query type for SteamAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSteamAccount extends EntityPathBase<SteamAccount> {

    private static final long serialVersionUID = -1546911026L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSteamAccount steamAccount = new QSteamAccount("steamAccount");

    public final StringPath avatar = createString("avatar");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath personaName = createString("personaName");

    public final StringPath steamId = createString("steamId");

    public final com.gamematcher.entity.QUser user;

    public QSteamAccount(String variable) {
        this(SteamAccount.class, forVariable(variable), INITS);
    }

    public QSteamAccount(Path<? extends SteamAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSteamAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSteamAccount(PathMetadata metadata, PathInits inits) {
        this(SteamAccount.class, metadata, inits);
    }

    public QSteamAccount(Class<? extends SteamAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.gamematcher.entity.QUser(forProperty("user")) : null;
    }

}

