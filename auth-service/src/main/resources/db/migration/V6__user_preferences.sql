-- User preferences surfaced on Account → Preferences tab.
--
-- All five columns are nullable / default-empty so existing users don't
-- need a backfill — the mobile reads "no preference" as a benign default
-- (USD, no flagged allergens, etc.). Language gets its own follow-up
-- once i18n is in place; until then the mobile renders that row as
-- "Coming soon".

ALTER TABLE users
    ADD COLUMN default_store_id    BIGINT,
    ADD COLUMN currency_code       VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD COLUMN dietary_prefs       TEXT[]     NOT NULL DEFAULT ARRAY[]::TEXT[],
    ADD COLUMN allergens           TEXT[]     NOT NULL DEFAULT ARRAY[]::TEXT[],
    ADD COLUMN notification_prefs  JSONB      NOT NULL DEFAULT '{}'::JSONB;

CREATE INDEX idx_users_default_store ON users(default_store_id);
