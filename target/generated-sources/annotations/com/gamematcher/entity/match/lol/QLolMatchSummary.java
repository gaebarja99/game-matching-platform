package com.gamematcher.entity.match.lol;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLolMatchSummary is a Querydsl query type for LolMatchSummary
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLolMatchSummary extends EntityPathBase<LolMatchSummary> {

    private static final long serialVersionUID = -352686644L;

    public static final QLolMatchSummary lolMatchSummary = new QLolMatchSummary("lolMatchSummary");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final StringPath championName = createString("championName");

    public final NumberPath<Integer> cs = createNumber("cs", Integer.class);

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final NumberPath<Long> gameCreation = createNumber("gameCreation", Long.class);

    public final NumberPath<Integer> gameDuration = createNumber("gameDuration", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final StringPath matchId = createString("matchId");

    public final StringPath puuid = createString("puuid");

    public final StringPath teamPosition = createString("teamPosition");

    public final NumberPath<Integer> totalDamage = createNumber("totalDamage", Integer.class);

    public final NumberPath<Integer> visionScore = createNumber("visionScore", Integer.class);

    public final BooleanPath win = createBoolean("win");

    public QLolMatchSummary(String variable) {
        super(LolMatchSummary.class, forVariable(variable));
    }

    public QLolMatchSummary(Path<? extends LolMatchSummary> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLolMatchSummary(PathMetadata metadata) {
        super(LolMatchSummary.class, metadata);
    }

}

