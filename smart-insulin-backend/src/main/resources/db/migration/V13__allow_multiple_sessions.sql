-- Allow up to N concurrent sessions per user (replaces the single-session model).
-- The one-session-per-user invariant was enforced by a UNIQUE(user_id) constraint —
-- drop it so a user can hold several refresh tokens at once.
ALTER TABLE refresh_token DROP CONSTRAINT uq_refresh_token_user_id;

-- created_at orders sessions by age so the oldest can be evicted past the limit.
ALTER TABLE refresh_token
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

-- The dropped UNIQUE constraint also provided the user_id index; restore a plain one
-- for the per-user lookups (count / order-by-age).
CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);
