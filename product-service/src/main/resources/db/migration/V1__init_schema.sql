CREATE TABLE categories (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(250) NOT NULL,
    slug      VARCHAR(255) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories(id)
);

CREATE TABLE products (
    id                     BIGSERIAL PRIMARY KEY,
    barcode                VARCHAR(14) NOT NULL UNIQUE,
    name                   VARCHAR(255) NOT NULL,
    brand                  VARCHAR(150),
    price                  NUMERIC(19,4) NOT NULL,
    currency_code          CHAR(3) NOT NULL DEFAULT 'EUR',
    category_id            BIGINT REFERENCES categories(id),
    aisle_number           INTEGER,
    image_url              VARCHAR(512),
    nutriscore_grade       CHAR(1),
    nova_group             SMALLINT,
    ingredients_text       TEXT,
    allergen_codes         TEXT[],
    stock_quantity         INTEGER NOT NULL DEFAULT 100,
    rfid_security_required BOOLEAN NOT NULL DEFAULT FALSE,
    nutriments             JSONB,
    attributes             JSONB,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_products_aisle ON products(aisle_number);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_attributes_gin ON products USING GIN (attributes);
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_products_name_trgm ON products USING GIN (name gin_trgm_ops);
