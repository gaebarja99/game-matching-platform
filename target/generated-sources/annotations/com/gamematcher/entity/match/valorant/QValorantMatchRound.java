package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantMatchRound is a Querydsl query type for ValorantMatchRound
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantMatchRound extends EntityPathBase<ValorantMatchRound> {

    private static final long serialVersionUID = -982957410L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantMatchRound valorantMatchRound = new QValorantMatchRound("valorantMatchRound");

    public final BooleanPath bombDefused = createBoolean("bombDefused");

    public final BooleanPath bombPlanted = createBoolean("bombPlanted");

    public final StringPath defusedByPuuid = createString("defusedByPuuid");

    public final NumberPath<Integer> defuseLocationX = createNumber("defuseLocationX", Integer.class);

    public final NumberPath<Integer> defuseLocationY = createNumber("defuseLocationY", Integer.class);

    public final NumberPath<Integer> defuseTimeInRound = createNumber("defuseTimeInRound", Integer.class);

    public final StringPath endType = createString("endType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QValorantMatch match;

    public final StringPath plantedByPuuid = createString("plantedByPuuid");

    public final NumberPath<Integer> plantLocationX = createNumber("plantLocationX", Integer.class);

    public final NumberPath<Integer> plantLocationY = createNumber("plantLocationY", Integer.class);

    public final StringPath plantSite = createString("plantSite");

    public final NumberPath<Integer> plantTimeInRound = createNumber("plantTimeInRound", Integer.class);

    public final ListPath<ValorantRoundPlayerLocation, QValorantRoundPlayerLocation> playerLocations = this.<ValorantRoundPlayerLocation, QValorantRoundPlayerLocation>createList("playerLocations", ValorantRoundPlayerLocation.class, QValorantRoundPlayerLocation.class, PathInits.DIRECT2);

    public final ListPath<ValorantMatchRoundPlayer, QValorantMatchRoundPlayer> playerStats = this.<ValorantMatchRoundPlayer, QValorantMatchRoundPlayer>createList("playerStats", ValorantMatchRoundPlayer.class, QValorantMatchRoundPlayer.class, PathInits.DIRECT2);

    public final NumberPath<Integer> roundIndex = createNumber("roundIndex", Integer.class);

    public final StringPath winningTeam = createString("winningTeam");

    public QValorantMatchRound(String variable) {
        this(ValorantMatchRound.class, forVariable(variable), INITS);
    }

    public QValorantMatchRound(Path<? extends ValorantMatchRound> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantMatchRound(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantMatchRound(PathMetadata metadata, PathInits inits) {
        this(ValorantMatchRound.class, metadata, inits);
    }

    public QValorantMatchRound(Class<? extends ValorantMatchRound> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QValorantMatch(forProperty("match")) : null;
    }

}

