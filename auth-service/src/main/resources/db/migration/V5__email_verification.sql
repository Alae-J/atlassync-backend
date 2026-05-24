-- Email verification.
--
-- Users get a verified flag so we can gate sensitive flows (e.g. starting a shopping
-- session) on a confirmed email. The OTP challenge table grows a `purpose` column so
-- the same delivery + rate-limit pipeline can mint codes for either passwordless login
-- or one-shot email verification, without conflating their lifecycles.

ALTER TABLE users
    ADD COLUMN email_verified    BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified_at TIMESTAMPTZ;

CREATE INDEX idx_users_email_verified ON users(email_verified);

ALTER TABLE otp_challenges
    ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'LOGIN';

DROP INDEX IF EXISTS idx_otp_recipient_status;
CREATE INDEX idx_otp_purpose_recipient_status ON otp_challenges(purpose, recipient, status);
