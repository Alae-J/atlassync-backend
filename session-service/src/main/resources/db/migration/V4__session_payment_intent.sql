-- Track the live PaymentIntent attached to a session. Lets us reuse the
-- intent on retries and resume across app backgrounding, and lets us
-- refuse new intents when the session already has one open or has been
-- paid for. {@code stripe_intent_status} caches the last-seen status so
-- the reuse decision doesn't need a Stripe round-trip when the intent is
-- in a terminal local state.

ALTER TABLE shopping_sessions
    ADD COLUMN stripe_payment_intent_id VARCHAR(64),
    ADD COLUMN stripe_intent_status     VARCHAR(32);

CREATE INDEX idx_shopping_sessions_stripe_intent
        ON shopping_sessions (stripe_payment_intent_id);
