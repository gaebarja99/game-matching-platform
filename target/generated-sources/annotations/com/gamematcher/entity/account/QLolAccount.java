package com.gamematcher.entity.account;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLolAccount is a Querydsl query type for LolAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLolAccount extends EntityPathBase<LolAccount> {

    private static final long serialVersionUID = -1699576075L;

    public static final QLolAccount lolAccount = new QLolAccount("lolAccount");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath gameName = createString("gameName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath puuid = createString("puuid");

    public final StringPath tagLine = createString("tagLine");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QLolAccount(String variable) {
        super(LolAccount.class, forVariable(variable));
    }

    public QLolAccount(Path<? extends LolAccount> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLolAccount(PathMetadata metadata) {
        super(LolAccount.class, metadata);
    }

}

