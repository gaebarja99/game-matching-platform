package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantMatchPlayer is a Querydsl query type for ValorantMatchPlayer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantMatchPlayer extends EntityPathBase<ValorantMatchPlayer> {

    private static final long serialVersionUID = -467522607L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantMatchPlayer valorantMatchPlayer = new QValorantMatchPlayer("valorantMatchPlayer");

    public final NumberPath<Integer> abilityCCast = createNumber("abilityCCast", Integer.class);

    public final NumberPath<Integer> abilityECast = createNumber("abilityECast", Integer.class);

    public final NumberPath<Integer> abilityQCast = createNumber("abilityQCast", Integer.class);

    public final NumberPath<Integer> abilityXCast = createNumber("abilityXCast", Integer.class);

    public final NumberPath<Integer> afkRounds = createNumber("afkRounds", Integer.class);

    public final StringPath agent = createString("agent");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Integer> bodyshots = createNumber("bodyshots", Integer.class);

    public final NumberPath<Integer> currentTier = createNumber("currentTier", Integer.class);

    public final StringPath currentTierPatched = createString("currentTierPatched");

    public final NumberPath<Integer> damageMade = createNumber("damageMade", Integer.class);

    public final NumberPath<Integer> damageReceived = createNumber("damageReceived", Integer.class);

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final NumberPath<Integer> economySpentOverall = createNumber("economySpentOverall", Integer.class);

    public final NumberPath<Integer> friendlyFireIncoming = createNumber("friendlyFireIncoming", Integer.class);

    public final NumberPath<Integer> friendlyFireOutgoing = createNumber("friendlyFireOutgoing", Integer.class);

    public final NumberPath<Integer> headshots = createNumber("headshots", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final NumberPath<Integer> legshots = createNumber("legshots", Integer.class);

    public final NumberPath<Integer> level = createNumber("level", Integer.class);

    public final NumberPath<Integer> loadoutValueOverall = createNumber("loadoutValueOverall", Integer.class);

    public final QValorantMatch match;

    public final StringPath name = createString("name");

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Integer> roundsInSpawn = createNumber("roundsInSpawn", Integer.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final StringPath tag = createString("tag");

    public final StringPath team = createString("team");

    public final BooleanPath win = createBoolean("win");

    public QValorantMatchPlayer(String variable) {
        this(ValorantMatchPlayer.class, forVariable(variable), INITS);
    }

    public QValorantMatchPlayer(Path<? extends ValorantMatchPlayer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantMatchPlayer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantMatchPlayer(PathMetadata metadata, PathInits inits) {
        this(ValorantMatchPlayer.class, metadata, inits);
    }

    public QValorantMatchPlayer(Class<? extends ValorantMatchPlayer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QValorantMatch(forProperty("match")) : null;
    }

}

