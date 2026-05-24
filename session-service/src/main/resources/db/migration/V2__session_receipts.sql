-- Snapshot the cart at payment time so receipts are stable forever.
--
-- The cart-service holds its data in Redis with a TTL; once a session closes
-- we don't want the receipt to vanish when the cart key expires. So at
-- completePayment, the session-service pulls the cart and writes a row per
-- line item into session_line_items, plus rolls the totals onto the session
-- itself for fast history queries.

ALTER TABLE shopping_sessions
    ADD COLUMN total_amount DECIMAL(10, 2),
    ADD COLUMN item_count   INTEGER,
    ADD COLUMN completed_at TIMESTAMPTZ;

CREATE INDEX idx_ss_completed_at ON shopping_sessions(completed_at DESC);

CREATE TABLE session_line_items (
    id            BIGSERIAL    PRIMARY KEY,
    session_id    UUID         NOT NULL REFERENCES shopping_sessions(id) ON DELETE CASCADE,
    barcode       VARCHAR(64)  NOT NULL,
    product_name  VARCHAR(255) NOT NULL,
    quantity      INTEGER      NOT NULL,
    unit_price    DECIMAL(10, 2) NOT NULL,
    line_total    DECIMAL(10, 2) NOT NULL,
    image_url     VARCHAR(512),
    added_at      TIMESTAMPTZ
);

CREATE INDEX idx_sli_session_id ON session_line_items(session_id);
