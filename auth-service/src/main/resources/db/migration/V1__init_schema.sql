CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) NOT NULL UNIQUE,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE privileges (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE roles_privileges (
    role_id      BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    privilege_id BIGINT NOT NULL REFERENCES privileges(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, privilege_id)
);

CREATE TABLE refresh_tokens (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash        VARCHAR(64)  NOT NULL UNIQUE,
    family_id         UUID         NOT NULL,
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at        TIMESTAMPTZ  NOT NULL,
    revoked           BOOLEAN      NOT NULL DEFAULT FALSE,
    revocation_reason VARCHAR(32),
    device_info       VARCHAR(512)
);

CREATE INDEX idx_rt_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_rt_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_rt_expires_at ON refresh_tokens(expires_at);
