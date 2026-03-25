package com.gamematcher.entity.match.cs2;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCs2Match is a Querydsl query type for Cs2Match
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCs2Match extends EntityPathBase<Cs2Match> {

    private static final long serialVersionUID = 1655133000L;

    public static final QCs2Match cs2Match = new QCs2Match("cs2Match");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final NumberPath<Integer> headshotKills = createNumber("headshotKills", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final StringPath map = createString("map");

    public final StringPath matchId = createString("matchId");

    public final NumberPath<Integer> mvps = createNumber("mvps", Integer.class);

    public final NumberPath<Long> playedAt = createNumber("playedAt", Long.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final StringPath steamId = createString("steamId");

    public final BooleanPath win = createBoolean("win");

    public QCs2Match(String variable) {
        super(Cs2Match.class, forVariable(variable));
    }

    public QCs2Match(Path<? extends Cs2Match> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCs2Match(PathMetadata metadata) {
        super(Cs2Match.class, metadata);
    }

}

