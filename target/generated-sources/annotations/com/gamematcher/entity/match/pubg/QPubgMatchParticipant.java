package com.gamematcher.entity.match.pubg;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPubgMatchParticipant is a Querydsl query type for PubgMatchParticipant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPubgMatchParticipant extends EntityPathBase<PubgMatchParticipant> {

    private static final long serialVersionUID = -162229981L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPubgMatchParticipant pubgMatchParticipant = new QPubgMatchParticipant("pubgMatchParticipant");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Integer> boosts = createNumber("boosts", Integer.class);

    public final NumberPath<Double> damageDealt = createNumber("damageDealt", Double.class);

    public final NumberPath<Integer> dbnos = createNumber("dbnos", Integer.class);

    public final StringPath deathType = createString("deathType");

    public final NumberPath<Integer> headshotKills = createNumber("headshotKills", Integer.class);

    public final NumberPath<Integer> heals = createNumber("heals", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> killPlace = createNumber("killPlace", Integer.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final NumberPath<Integer> killStreaks = createNumber("killStreaks", Integer.class);

    public final NumberPath<Double> longestKill = createNumber("longestKill", Double.class);

    public final QPubgMatch match;

    public final StringPath name = createString("name");

    public final StringPath participantId = createString("participantId");

    public final StringPath playerId = createString("playerId");

    public final NumberPath<Integer> revives = createNumber("revives", Integer.class);

    public final NumberPath<Double> rideDistance = createNumber("rideDistance", Double.class);

    public final NumberPath<Integer> roadKills = createNumber("roadKills", Integer.class);

    public final NumberPath<Double> swimDistance = createNumber("swimDistance", Double.class);

    public final NumberPath<Integer> teamKills = createNumber("teamKills", Integer.class);

    public final NumberPath<Integer> timeSurvived = createNumber("timeSurvived", Integer.class);

    public final NumberPath<Integer> vehicleDestroys = createNumber("vehicleDestroys", Integer.class);

    public final NumberPath<Double> walkDistance = createNumber("walkDistance", Double.class);

    public final NumberPath<Integer> weaponsAcquired = createNumber("weaponsAcquired", Integer.class);

    public final BooleanPath win = createBoolean("win");

    public final NumberPath<Integer> winPlace = createNumber("winPlace", Integer.class);

    public QPubgMatchParticipant(String variable) {
        this(PubgMatchParticipant.class, forVariable(variable), INITS);
    }

    public QPubgMatchParticipant(Path<? extends PubgMatchParticipant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPubgMatchParticipant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPubgMatchParticipant(PathMetadata metadata, PathInits inits) {
        this(PubgMatchParticipant.class, metadata, inits);
    }

    public QPubgMatchParticipant(Class<? extends PubgMatchParticipant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QPubgMatch(forProperty("match")) : null;
    }

}

