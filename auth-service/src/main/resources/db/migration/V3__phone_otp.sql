-- Phone + OTP authentication
--
-- Phone-only users have no email, username, or password until they set them later.
-- email and username remain UNIQUE; Postgres treats NULLs as distinct so multiple
-- phone-only rows are allowed.

ALTER TABLE users
    ALTER COLUMN email    DROP NOT NULL,
    ALTER COLUMN username DROP NOT NULL,
    ALTER COLUMN password DROP NOT NULL,
    ADD  COLUMN phone     VARCHAR(20) UNIQUE;

CREATE INDEX idx_users_phone ON users(phone);

-- Either email or phone must be present for any user.
ALTER TABLE users
    ADD CONSTRAINT users_email_or_phone_present
        CHECK (email IS NOT NULL OR phone IS NOT NULL);

CREATE TABLE otp_challenges (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(20)  NOT NULL,
    code_hash       VARCHAR(64)  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    attempts        INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ  NOT NULL,
    consumed_at     TIMESTAMPTZ
);

CREATE INDEX idx_otp_phone_status ON otp_challenges(phone, status);
CREATE INDEX idx_otp_expires_at   ON otp_challenges(expires_at);
