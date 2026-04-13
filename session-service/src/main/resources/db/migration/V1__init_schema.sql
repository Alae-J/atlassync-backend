CREATE TABLE shopping_sessions (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      BIGINT       NOT NULL,
    store_id     BIGINT       NOT NULL DEFAULT 1,
    status       VARCHAR(16)  NOT NULL DEFAULT 'CREATED',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ,
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_ss_user_id ON shopping_sessions(user_id);
CREATE INDEX idx_ss_status ON shopping_sessions(status);

CREATE TABLE session_state_transitions (
    id               BIGSERIAL    PRIMARY KEY,
    session_id       UUID         NOT NULL REFERENCES shopping_sessions(id),
    from_state       VARCHAR(16)  NOT NULL,
    to_state         VARCHAR(16)  NOT NULL,
    transitioned_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    triggered_by     VARCHAR(100),
    reason           VARCHAR(512)
);

CREATE INDEX idx_sst_session_id ON session_state_transitions(session_id);

CREATE TABLE qr_tokens (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id  VARCHAR(36)  NOT NULL UNIQUE,
    session_id      UUID         NOT NULL REFERENCES shopping_sessions(id),
    token_type      VARCHAR(10)  NOT NULL,
    payload         TEXT         NOT NULL,
    vault_signature TEXT         NOT NULL,
    key_version     INTEGER      NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ  NOT NULL,
    used_at         TIMESTAMPTZ,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_qrt_correlation_id ON qr_tokens(correlation_id);
CREATE INDEX idx_qrt_session_id ON qr_tokens(session_id);
CREATE INDEX idx_qrt_expires_at ON qr_tokens(expires_at);
