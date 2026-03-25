package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QValorantMmrRecord is a Querydsl query type for ValorantMmrRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantMmrRecord extends EntityPathBase<ValorantMmrRecord> {

    private static final long serialVersionUID = -2025503922L;

    public static final QValorantMmrRecord valorantMmrRecord = new QValorantMmrRecord("valorantMmrRecord");

    public final StringPath bySeasonJson = createString("bySeasonJson");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> currentTier = createNumber("currentTier", Integer.class);

    public final StringPath currentTierPatched = createString("currentTierPatched");

    public final NumberPath<Integer> elo = createNumber("elo", Integer.class);

    public final NumberPath<Integer> gamesNeededForRating = createNumber("gamesNeededForRating", Integer.class);

    public final StringPath highestSeason = createString("highestSeason");

    public final NumberPath<Integer> highestTier = createNumber("highestTier", Integer.class);

    public final StringPath highestTierPatched = createString("highestTierPatched");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageLarge = createString("imageLarge");

    public final StringPath imageSmall = createString("imageSmall");

    public final NumberPath<Integer> mmrChangeToLastGame = createNumber("mmrChangeToLastGame", Integer.class);

    public final StringPath name = createString("name");

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Integer> rankingInTier = createNumber("rankingInTier", Integer.class);

    public final StringPath tag = createString("tag");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QValorantMmrRecord(String variable) {
        super(ValorantMmrRecord.class, forVariable(variable));
    }

    public QValorantMmrRecord(Path<? extends ValorantMmrRecord> path) {
        super(path.getType(), path.getMetadata());
    }

    public QValorantMmrRecord(PathMetadata metadata) {
        super(ValorantMmrRecord.class, metadata);
    }

}

