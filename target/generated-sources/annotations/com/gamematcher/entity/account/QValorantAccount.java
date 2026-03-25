package com.gamematcher.entity.account;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QValorantAccount is a Querydsl query type for ValorantAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantAccount extends EntityPathBase<ValorantAccount> {

    private static final long serialVersionUID = -1959010215L;

    public static final QValorantAccount valorantAccount = new QValorantAccount("valorantAccount");

    public final NumberPath<Integer> accountLevel = createNumber("accountLevel", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> apiCachedAt = createDateTime("apiCachedAt", java.time.LocalDateTime.class);

    public final StringPath cardId = createString("cardId");

    public final StringPath cardLarge = createString("cardLarge");

    public final StringPath cardSmall = createString("cardSmall");

    public final StringPath cardWide = createString("cardWide");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath lastUpdate = createString("lastUpdate");

    public final NumberPath<Long> lastUpdateRaw = createNumber("lastUpdateRaw", Long.class);

    public final StringPath name = createString("name");

    public final StringPath puuid = createString("puuid");

    public final StringPath region = createString("region");

    public final StringPath tag = createString("tag");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QValorantAccount(String variable) {
        super(ValorantAccount.class, forVariable(variable));
    }

    public QValorantAccount(Path<? extends ValorantAccount> path) {
        super(path.getType(), path.getMetadata());
    }

    public QValorantAccount(PathMetadata metadata) {
        super(ValorantAccount.class, metadata);
    }

}

