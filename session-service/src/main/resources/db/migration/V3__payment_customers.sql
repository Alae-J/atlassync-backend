-- Correlates an AtlasSync user to their long-lived Stripe Customer object.
-- One row per (user_id, stripe_customer_id) pair. Lazy-created on first
-- checkout — registration stays fast and offline-friendly.
--
-- session-service owns this table because Stripe customers are a
-- payment-flow concern; auth-service stays Stripe-unaware. The unique
-- constraint on user_id enforces "one customer per user" so we never
-- accidentally create a second customer mid-session.

CREATE TABLE payment_customers (
    user_id            BIGINT PRIMARY KEY,
    stripe_customer_id VARCHAR(64) NOT NULL UNIQUE,
    email_at_creation  VARCHAR(255),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_customers_stripe_id
        ON payment_customers (stripe_customer_id);
