package com.gamematcher.entity.community;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPost is a Querydsl query type for Post
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPost extends EntityPathBase<Post> {

    private static final long serialVersionUID = -609853067L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPost post = new QPost("post");

    public final ListPath<PostAttachment, QPostAttachment> attachments = this.<PostAttachment, QPostAttachment>createList("attachments", PostAttachment.class, QPostAttachment.class, PathInits.DIRECT2);

    public final com.gamematcher.entity.QUser author;

    public final EnumPath<com.gamematcher.constant.community.BoardCategory> boardCategory = createEnum("boardCategory", com.gamematcher.constant.community.BoardCategory.class);

    public final NumberPath<Integer> commentCount = createNumber("commentCount", Integer.class);

    public final ListPath<Comment, QComment> comments = this.<Comment, QComment>createList("comments", Comment.class, QComment.class, PathInits.DIRECT2);

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isNotice = createBoolean("isNotice");

    public final NumberPath<Integer> likeCount = createNumber("likeCount", Integer.class);

    public final NumberPath<Integer> notRecommendCount = createNumber("notRecommendCount", Integer.class);

    public final ListPath<PostHashtag, QPostHashtag> postHashtags = this.<PostHashtag, QPostHashtag>createList("postHashtags", PostHashtag.class, QPostHashtag.class, PathInits.DIRECT2);

    public final NumberPath<Integer> recommendCount = createNumber("recommendCount", Integer.class);

    public final NumberPath<Integer> reportCount = createNumber("reportCount", Integer.class);

    public final EnumPath<com.gamematcher.constant.community.PostStatus> status = createEnum("status", com.gamematcher.constant.community.PostStatus.class);

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> viewCount = createNumber("viewCount", Integer.class);

    public QPost(String variable) {
        this(Post.class, forVariable(variable), INITS);
    }

    public QPost(Path<? extends Post> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPost(PathMetadata metadata, PathInits inits) {
        this(Post.class, metadata, inits);
    }

    public QPost(Class<? extends Post> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.gamematcher.entity.QUser(forProperty("author")) : null;
    }

}

