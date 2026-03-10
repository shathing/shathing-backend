package com.shathing.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEmailAuthToken is a Querydsl query type for EmailAuthToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEmailAuthToken extends EntityPathBase<EmailAuthToken> {

    private static final long serialVersionUID = 1706473095L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEmailAuthToken emailAuthToken = new QEmailAuthToken("emailAuthToken");

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMember member;

    public final StringPath tokenHash = createString("tokenHash");

    public QEmailAuthToken(String variable) {
        this(EmailAuthToken.class, forVariable(variable), INITS);
    }

    public QEmailAuthToken(Path<? extends EmailAuthToken> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEmailAuthToken(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEmailAuthToken(PathMetadata metadata, PathInits inits) {
        this(EmailAuthToken.class, metadata, inits);
    }

    public QEmailAuthToken(Class<? extends EmailAuthToken> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new QMember(forProperty("member")) : null;
    }

}

