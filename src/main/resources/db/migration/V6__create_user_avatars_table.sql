CREATE TABLE user_avatars (
    user_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    content_type  VARCHAR(50)  NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    width         INTEGER      NOT NULL,
    height        INTEGER      NOT NULL,
    uploaded_at   TIMESTAMP    NOT NULL
);
