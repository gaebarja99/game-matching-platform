package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantKillEvent is a Querydsl query type for ValorantKillEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantKillEvent extends EntityPathBase<ValorantKillEvent> {

    private static final long serialVersionUID = -1401562265L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantKillEvent valorantKillEvent = new QValorantKillEvent("valorantKillEvent");

    public final ListPath<ValorantKillAssistant, QValorantKillAssistant> assistants = this.<ValorantKillAssistant, QValorantKillAssistant>createList("assistants", ValorantKillAssistant.class, QValorantKillAssistant.class, PathInits.DIRECT2);

    public final StringPath damageWeaponId = createString("damageWeaponId");

    public final StringPath damageWeaponName = createString("damageWeaponName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath killerDisplayName = createString("killerDisplayName");

    public final StringPath killerPuuid = createString("killerPuuid");

    public final StringPath killerTeam = createString("killerTeam");

    public final NumberPath<Integer> killTimeInMatch = createNumber("killTimeInMatch", Integer.class);

    public final NumberPath<Integer> killTimeInRound = createNumber("killTimeInRound", Integer.class);

    public final QValorantMatch match;

    public final ListPath<ValorantKillEventPlayerLocation, QValorantKillEventPlayerLocation> playerLocations = this.<ValorantKillEventPlayerLocation, QValorantKillEventPlayerLocation>createList("playerLocations", ValorantKillEventPlayerLocation.class, QValorantKillEventPlayerLocation.class, PathInits.DIRECT2);

    public final NumberPath<Integer> roundNumber = createNumber("roundNumber", Integer.class);

    public final BooleanPath secondaryFireMode = createBoolean("secondaryFireMode");

    public final NumberPath<Integer> victimDeathX = createNumber("victimDeathX", Integer.class);

    public final NumberPath<Integer> victimDeathY = createNumber("victimDeathY", Integer.class);

    public final StringPath victimDisplayName = createString("victimDisplayName");

    public final StringPath victimPuuid = createString("victimPuuid");

    public final StringPath victimTeam = createString("victimTeam");

    public QValorantKillEvent(String variable) {
        this(ValorantKillEvent.class, forVariable(variable), INITS);
    }

    public QValorantKillEvent(Path<? extends ValorantKillEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantKillEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantKillEvent(PathMetadata metadata, PathInits inits) {
        this(ValorantKillEvent.class, metadata, inits);
    }

    public QValorantKillEvent(Class<? extends ValorantKillEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QValorantMatch(forProperty("match")) : null;
    }

}

