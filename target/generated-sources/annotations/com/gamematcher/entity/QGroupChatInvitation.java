package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGroupChatInvitation is a Querydsl query type for GroupChatInvitation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroupChatInvitation extends EntityPathBase<GroupChatInvitation> {

    private static final long serialVersionUID = 462567808L;

    public static final QGroupChatInvitation groupChatInvitation = new QGroupChatInvitation("groupChatInvitation");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> fromUserId = createNumber("fromUserId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> roomId = createNumber("roomId", Long.class);

    public final EnumPath<GroupChatInvitation.InvitationStatus> status = createEnum("status", GroupChatInvitation.InvitationStatus.class);

    public final NumberPath<Long> toUserId = createNumber("toUserId", Long.class);

    public QGroupChatInvitation(String variable) {
        super(GroupChatInvitation.class, forVariable(variable));
    }

    public QGroupChatInvitation(Path<? extends GroupChatInvitation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGroupChatInvitation(PathMetadata metadata) {
        super(GroupChatInvitation.class, metadata);
    }

}

