package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantRoundPlayerLocation is a Querydsl query type for ValorantRoundPlayerLocation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantRoundPlayerLocation extends EntityPathBase<ValorantRoundPlayerLocation> {

    private static final long serialVersionUID = 10408623L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantRoundPlayerLocation valorantRoundPlayerLocation = new QValorantRoundPlayerLocation("valorantRoundPlayerLocation");

    public final StringPath eventType = createString("eventType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> locationX = createNumber("locationX", Integer.class);

    public final NumberPath<Integer> locationY = createNumber("locationY", Integer.class);

    public final StringPath playerDisplayName = createString("playerDisplayName");

    public final StringPath playerPuuid = createString("playerPuuid");

    public final StringPath playerTeam = createString("playerTeam");

    public final QValorantMatchRound round;

    public final NumberPath<Double> viewRadians = createNumber("viewRadians", Double.class);

    public QValorantRoundPlayerLocation(String variable) {
        this(ValorantRoundPlayerLocation.class, forVariable(variable), INITS);
    }

    public QValorantRoundPlayerLocation(Path<? extends ValorantRoundPlayerLocation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantRoundPlayerLocation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantRoundPlayerLocation(PathMetadata metadata, PathInits inits) {
        this(ValorantRoundPlayerLocation.class, metadata, inits);
    }

    public QValorantRoundPlayerLocation(Class<? extends ValorantRoundPlayerLocation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.round = inits.isInitialized("round") ? new QValorantMatchRound(forProperty("round"), inits.get("round")) : null;
    }

}

