-- Per-user shopping lists surfaced on the mobile Lists tab. Items carry
-- a barcode + qty; the mobile computes price estimates client-side once
-- the product-service is wired up. Cascade deletes keep the two tables
-- in sync without requiring explicit cleanup in application code.

CREATE TABLE shopping_lists (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name         VARCHAR(80)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_shopping_lists_user ON shopping_lists(user_id);

CREATE TABLE shopping_list_items (
    list_id     BIGINT       NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    position    INT          NOT NULL,
    barcode     VARCHAR(64)  NOT NULL,
    qty         INT          NOT NULL CHECK (qty > 0),
    PRIMARY KEY (list_id, position)
);
CREATE INDEX idx_shopping_list_items_list ON shopping_list_items(list_id);
