package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantKillEventPlayerLocation is a Querydsl query type for ValorantKillEventPlayerLocation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantKillEventPlayerLocation extends EntityPathBase<ValorantKillEventPlayerLocation> {

    private static final long serialVersionUID = 1920816381L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantKillEventPlayerLocation valorantKillEventPlayerLocation = new QValorantKillEventPlayerLocation("valorantKillEventPlayerLocation");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QValorantKillEvent killEvent;

    public final NumberPath<Integer> locationX = createNumber("locationX", Integer.class);

    public final NumberPath<Integer> locationY = createNumber("locationY", Integer.class);

    public final StringPath playerDisplayName = createString("playerDisplayName");

    public final StringPath playerPuuid = createString("playerPuuid");

    public final StringPath playerTeam = createString("playerTeam");

    public final NumberPath<Double> viewRadians = createNumber("viewRadians", Double.class);

    public QValorantKillEventPlayerLocation(String variable) {
        this(ValorantKillEventPlayerLocation.class, forVariable(variable), INITS);
    }

    public QValorantKillEventPlayerLocation(Path<? extends ValorantKillEventPlayerLocation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantKillEventPlayerLocation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantKillEventPlayerLocation(PathMetadata metadata, PathInits inits) {
        this(ValorantKillEventPlayerLocation.class, metadata, inits);
    }

    public QValorantKillEventPlayerLocation(Class<? extends ValorantKillEventPlayerLocation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.killEvent = inits.isInitialized("killEvent") ? new QValorantKillEvent(forProperty("killEvent"), inits.get("killEvent")) : null;
    }

}

