CREATE TABLE cart_items (
    id                BIGSERIAL     PRIMARY KEY,
    session_id        VARCHAR(128)  NOT NULL,
    product_id        VARCHAR(64)   NOT NULL,
    product_name      VARCHAR(255),
    quantity          INTEGER       NOT NULL DEFAULT 1,
    price_at_addition NUMERIC(19,4) NOT NULL,
    image_url         VARCHAR(512),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, product_id)
);

CREATE INDEX idx_cart_items_session_id ON cart_items(session_id);
