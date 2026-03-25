package com.gamematcher.entity.account;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBlizzardAccount is a Querydsl query type for BlizzardAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBlizzardAccount extends EntityPathBase<BlizzardAccount> {

    private static final long serialVersionUID = 1037879496L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBlizzardAccount blizzardAccount = new QBlizzardAccount("blizzardAccount");

    public final StringPath accountId = createString("accountId");

    public final StringPath battleTag = createString("battleTag");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath region = createString("region");

    public final com.gamematcher.entity.QUser user;

    public QBlizzardAccount(String variable) {
        this(BlizzardAccount.class, forVariable(variable), INITS);
    }

    public QBlizzardAccount(Path<? extends BlizzardAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBlizzardAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBlizzardAccount(PathMetadata metadata, PathInits inits) {
        this(BlizzardAccount.class, metadata, inits);
    }

    public QBlizzardAccount(Class<? extends BlizzardAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.gamematcher.entity.QUser(forProperty("user")) : null;
    }

}

