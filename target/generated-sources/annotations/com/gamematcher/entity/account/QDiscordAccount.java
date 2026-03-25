package com.gamematcher.entity.account;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDiscordAccount is a Querydsl query type for DiscordAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDiscordAccount extends EntityPathBase<DiscordAccount> {

    private static final long serialVersionUID = -1875310798L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDiscordAccount discordAccount = new QDiscordAccount("discordAccount");

    public final StringPath avatar = createString("avatar");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath discordId = createString("discordId");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.gamematcher.entity.QUser user;

    public final StringPath username = createString("username");

    public QDiscordAccount(String variable) {
        this(DiscordAccount.class, forVariable(variable), INITS);
    }

    public QDiscordAccount(Path<? extends DiscordAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDiscordAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDiscordAccount(PathMetadata metadata, PathInits inits) {
        this(DiscordAccount.class, metadata, inits);
    }

    public QDiscordAccount(Class<? extends DiscordAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.gamematcher.entity.QUser(forProperty("user")) : null;
    }

}

