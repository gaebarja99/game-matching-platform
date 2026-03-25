package com.gamematcher.entity.match.apex;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QApexMatch is a Querydsl query type for ApexMatch
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QApexMatch extends EntityPathBase<ApexMatch> {

    private static final long serialVersionUID = -1657913104L;

    public static final QApexMatch apexMatch = new QApexMatch("apexMatch");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Integer> damage = createNumber("damage", Integer.class);

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final StringPath legend = createString("legend");

    public final StringPath map = createString("map");

    public final StringPath matchId = createString("matchId");

    public final NumberPath<Integer> placement = createNumber("placement", Integer.class);

    public final NumberPath<Long> playedAt = createNumber("playedAt", Long.class);

    public final StringPath uid = createString("uid");

    public final BooleanPath win = createBoolean("win");

    public QApexMatch(String variable) {
        super(ApexMatch.class, forVariable(variable));
    }

    public QApexMatch(Path<? extends ApexMatch> path) {
        super(path.getType(), path.getMetadata());
    }

    public QApexMatch(PathMetadata metadata) {
        super(ApexMatch.class, metadata);
    }

}

