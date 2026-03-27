CREATE TABLE refresh_token (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(512) NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id     BIGINT NOT NULL,
    CONSTRAINT uq_refresh_token_token   UNIQUE (token),
    CONSTRAINT uq_refresh_token_user_id UNIQUE (user_id),
    CONSTRAINT fk_refresh_token_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
