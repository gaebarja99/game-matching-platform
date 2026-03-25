package com.gamematcher.entity.account;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLolSummonerProfile is a Querydsl query type for LolSummonerProfile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLolSummonerProfile extends EntityPathBase<LolSummonerProfile> {

    private static final long serialVersionUID = 1522125123L;

    public static final QLolSummonerProfile lolSummonerProfile = new QLolSummonerProfile("lolSummonerProfile");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> profileIconId = createNumber("profileIconId", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Long> revisionDate = createNumber("revisionDate", Long.class);

    public final NumberPath<Integer> summonerLevel = createNumber("summonerLevel", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QLolSummonerProfile(String variable) {
        super(LolSummonerProfile.class, forVariable(variable));
    }

    public QLolSummonerProfile(Path<? extends LolSummonerProfile> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLolSummonerProfile(PathMetadata metadata) {
        super(LolSummonerProfile.class, metadata);
    }

}

