package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantMatchRoundPlayer is a Querydsl query type for ValorantMatchRoundPlayer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantMatchRoundPlayer extends EntityPathBase<ValorantMatchRoundPlayer> {

    private static final long serialVersionUID = -492053729L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantMatchRoundPlayer valorantMatchRoundPlayer = new QValorantMatchRoundPlayer("valorantMatchRoundPlayer");

    public final NumberPath<Integer> abilityCCasts = createNumber("abilityCCasts", Integer.class);

    public final NumberPath<Integer> abilityECasts = createNumber("abilityECasts", Integer.class);

    public final NumberPath<Integer> abilityQCasts = createNumber("abilityQCasts", Integer.class);

    public final NumberPath<Integer> abilityXCasts = createNumber("abilityXCasts", Integer.class);

    public final NumberPath<Integer> bodyshots = createNumber("bodyshots", Integer.class);

    public final NumberPath<Integer> damage = createNumber("damage", Integer.class);

    public final ListPath<ValorantRoundPlayerDamageEvent, QValorantRoundPlayerDamageEvent> damageEvents = this.<ValorantRoundPlayerDamageEvent, QValorantRoundPlayerDamageEvent>createList("damageEvents", ValorantRoundPlayerDamageEvent.class, QValorantRoundPlayerDamageEvent.class, PathInits.DIRECT2);

    public final NumberPath<Integer> economyRemaining = createNumber("economyRemaining", Integer.class);

    public final NumberPath<Integer> economySpent = createNumber("economySpent", Integer.class);

    public final NumberPath<Integer> headshots = createNumber("headshots", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final NumberPath<Integer> legshots = createNumber("legshots", Integer.class);

    public final NumberPath<Integer> loadoutValue = createNumber("loadoutValue", Integer.class);

    public final StringPath playerDisplayName = createString("playerDisplayName");

    public final StringPath playerPuuid = createString("playerPuuid");

    public final StringPath playerTeam = createString("playerTeam");

    public final QValorantMatchRound round;

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final BooleanPath stayedInSpawn = createBoolean("stayedInSpawn");

    public final BooleanPath wasAfk = createBoolean("wasAfk");

    public final BooleanPath wasPenalized = createBoolean("wasPenalized");

    public QValorantMatchRoundPlayer(String variable) {
        this(ValorantMatchRoundPlayer.class, forVariable(variable), INITS);
    }

    public QValorantMatchRoundPlayer(Path<? extends ValorantMatchRoundPlayer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantMatchRoundPlayer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantMatchRoundPlayer(PathMetadata metadata, PathInits inits) {
        this(ValorantMatchRoundPlayer.class, metadata, inits);
    }

    public QValorantMatchRoundPlayer(Class<? extends ValorantMatchRoundPlayer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.round = inits.isInitialized("round") ? new QValorantMatchRound(forProperty("round"), inits.get("round")) : null;
    }

}

