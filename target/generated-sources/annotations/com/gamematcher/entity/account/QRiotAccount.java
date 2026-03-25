package com.gamematcher.entity.account;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRiotAccount is a Querydsl query type for RiotAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRiotAccount extends EntityPathBase<RiotAccount> {

    private static final long serialVersionUID = -424966656L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRiotAccount riotAccount = new QRiotAccount("riotAccount");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath gameName = createString("gameName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath puuid = createString("puuid");

    public final StringPath tagLine = createString("tagLine");

    public final com.gamematcher.entity.QUser user;

    public QRiotAccount(String variable) {
        this(RiotAccount.class, forVariable(variable), INITS);
    }

    public QRiotAccount(Path<? extends RiotAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRiotAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRiotAccount(PathMetadata metadata, PathInits inits) {
        this(RiotAccount.class, metadata, inits);
    }

    public QRiotAccount(Class<? extends RiotAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.gamematcher.entity.QUser(forProperty("user")) : null;
    }

}

