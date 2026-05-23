-- Phygital-aligned catalog seed.
--
-- The original V2 seed is French-language, EUR-priced, and uses aisles 1-5
-- (Produce / Dairy / Pasta / Drinks / Snacks). The mobile design lives in
-- USD and expects aisles 1, 5, 8, 9, 11, 12 (Produce, Dairy, Bakery, Pantry,
-- Seafood, Halal counter), plus structured dietary / allergen metadata so
-- the Product Detail allergen banner, dietary chips, and the scan peek
-- card's variants all light up.
--
-- This migration adds a parallel set of products designed against that
-- language. Includes the three SIMULATE_BARCODES the mobile scan flow
-- picks from (1234567890 / 2345678901 / 3456789012) so the demo exercises
-- a normal scan, an allergen-locked scan, and an RFID scan respectively.

-- Categories the new entries belong to. id values picked to leave room
-- after the V2 1-5 range (which is bumped via setval below).
INSERT INTO categories (id, name, slug, parent_id) VALUES
(6,  'Bakery',  'bakery',  NULL),
(7,  'Seafood', 'seafood', NULL),
(8,  'Meat',    'meat',    NULL);

-- ─── Aisle 1 · Produce ───────────────────────────────────────────────
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, nutriscore_grade, stock_quantity, rfid_security_required, ingredients_text, allergen_codes, nutriments, attributes) VALUES
('1234567890',    'Bananas',          'Fairtrade Co-op',         0.59,  'USD', 1, 1, 'a', 100, FALSE,
 'Fresh bananas.',
 ARRAY[]::TEXT[],
 '{"energy-kcal_100g":89,"sugars_100g":12.2,"fat_100g":0.3,"salt_100g":0.0}'::JSONB,
 '{"unit":"lb","dietary":["Halal","Vegan","Gluten-free"]}'::JSONB),
('0680569123459', 'Avocado',           'Marina Farms',           1.25,  'USD', 1, 1, 'a', 100, FALSE,
 'Fresh hass avocado.',
 ARRAY[]::TEXT[],
 '{"energy-kcal_100g":160,"sugars_100g":0.7,"fat_100g":14.7,"salt_100g":0.0}'::JSONB,
 '{"unit":"ea","dietary":["Halal","Vegan","Gluten-free"]}'::JSONB),
('0680569123466', 'Roma tomatoes',     'Local · Wadi El Natrun', 1.99,  'USD', 1, 1, 'a', 100, FALSE,
 'Fresh roma tomatoes.',
 ARRAY[]::TEXT[],
 '{"energy-kcal_100g":18,"sugars_100g":2.6,"fat_100g":0.2,"salt_100g":0.0}'::JSONB,
 '{"unit":"lb","dietary":["Halal","Vegan","Gluten-free"]}'::JSONB),
('0680569123473', 'Yellow onions',     'Local · Beheira',        1.49,  'USD', 1, 1, 'a', 100, FALSE,
 'Fresh yellow onions.',
 ARRAY[]::TEXT[],
 '{"energy-kcal_100g":40,"sugars_100g":4.2,"fat_100g":0.1,"salt_100g":0.0}'::JSONB,
 '{"unit":"lb","dietary":["Halal","Vegan","Gluten-free"]}'::JSONB),
('0680569123480', 'Honeycrisp apples', 'Wenatchee Orchards',     2.49,  'USD', 1, 1, 'a', 100, FALSE,
 'Fresh honeycrisp apples.',
 ARRAY[]::TEXT[],
 '{"energy-kcal_100g":52,"sugars_100g":10.4,"fat_100g":0.2,"salt_100g":0.0}'::JSONB,
 '{"unit":"lb","dietary":["Halal","Vegan","Gluten-free"]}'::JSONB);

-- ─── Aisle 5 · Dairy ─────────────────────────────────────────────────
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, nutriscore_grade, stock_quantity, rfid_security_required, ingredients_text, allergen_codes, nutriments, attributes) VALUES
-- 2345678901 → allergen-locked scan variant (Milk hits the user's flagged allergens).
('2345678901',    'Whole milk',        'Juhayna',                4.29,  'USD', 2, 5, 'b', 100, FALSE,
 'Pasteurised whole cow''s milk. Vitamin D added.',
 ARRAY['Milk'],
 '{"energy-kcal_100g":61,"sugars_100g":4.8,"fat_100g":3.3,"salt_100g":0.1}'::JSONB,
 '{"unit":"gal","dietary":["Halal","Vegetarian"]}'::JSONB),
('0680569123497', 'Eggs, dozen',       'Daltex Free-Range',      6.99,  'USD', 2, 5, 'a', 100, FALSE,
 'Free-range chicken eggs.',
 ARRAY['Eggs'],
 '{"energy-kcal_100g":155,"sugars_100g":1.1,"fat_100g":11.0,"salt_100g":0.4}'::JSONB,
 '{"unit":"dz","dietary":["Halal","Vegetarian"]}'::JSONB),
-- Aged cheddar is OUT OF STOCK to exercise the disabled-CTA / Notify-me state.
('0680569123503', 'Aged cheddar',      'Domty Reserve',          7.50,  'USD', 2, 5, 'd', 0,   FALSE,
 'Pasteurised cow''s milk, salt, microbial rennet, cheese cultures. Aged 24 months.',
 ARRAY['Milk'],
 '{"energy-kcal_100g":402,"sugars_100g":0.1,"fat_100g":33.1,"salt_100g":1.8}'::JSONB,
 '{"unit":"block","dietary":["Halal","Vegetarian"]}'::JSONB),
('0680569123510', 'Greek yogurt',      'Juhayna Mix',            5.99,  'USD', 2, 5, 'a', 100, FALSE,
 'Strained pasteurised milk, live yogurt cultures (S. thermophilus, L. bulgaricus).',
 ARRAY['Milk'],
 '{"energy-kcal_100g":97,"sugars_100g":3.6,"fat_100g":5.0,"salt_100g":0.1}'::JSONB,
 '{"unit":"tub","dietary":["Halal","Vegetarian"]}'::JSONB);

-- ─── Aisle 8 · Bakery ────────────────────────────────────────────────
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, nutriscore_grade, stock_quantity, rfid_security_required, ingredients_text, allergen_codes, nutriments, attributes) VALUES
('0680569123527', 'Sourdough loaf',    'Anbar Bakery',           5.50,  'USD', 6, 8, 'b', 100, FALSE,
 'Wheat flour, water, sea salt, sourdough starter (wheat flour, water).',
 ARRAY['Gluten','Wheat'],
 '{"energy-kcal_100g":247,"sugars_100g":1.4,"fat_100g":1.0,"salt_100g":1.2}'::JSONB,
 '{"unit":"ea","dietary":["Halal","Vegan"],"about":"Baked on-site each morning. Picked up warm — best within 48h."}'::JSONB);

-- ─── Aisle 9 · Pantry ────────────────────────────────────────────────
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, nutriscore_grade, stock_quantity, rfid_security_required, ingredients_text, allergen_codes, nutriments, attributes) VALUES
('0680569123534', 'Penne pasta',       'Regina',                 2.49,  'USD', 3, 9, 'c', 100, FALSE,
 'Durum wheat semolina, water.',
 ARRAY['Gluten','Wheat'],
 '{"energy-kcal_100g":371,"sugars_100g":3.2,"fat_100g":1.5,"salt_100g":0.0}'::JSONB,
 '{"unit":"box","dietary":["Halal","Vegetarian","Vegan"]}'::JSONB),
('0680569123541', 'Olive oil',         'Crete Gold',            12.99,  'USD', 3, 9, 'c', 100, FALSE,
 'Extra virgin olive oil, cold-pressed. First harvest, single estate.',
 ARRAY[]::TEXT[],
 '{"energy-kcal_100g":884,"sugars_100g":0.0,"fat_100g":100,"salt_100g":0.0}'::JSONB,
 '{"unit":"bottle","dietary":["Halal","Vegan","Gluten-free"]}'::JSONB),
('0680569123558', 'Whole-bean coffee', 'Beanbelt Roastery',     13.50,  'USD', 4, 9, 'b', 100, FALSE,
 'Single-origin Ethiopia Yirgacheffe, medium roast.',
 ARRAY[]::TEXT[],
 '{"energy-kcal_100g":2,"sugars_100g":0.0,"fat_100g":0.0,"salt_100g":0.0}'::JSONB,
 '{"unit":"bag","dietary":["Halal","Vegan","Gluten-free"]}'::JSONB);

-- ─── Aisle 11 · Seafood ──────────────────────────────────────────────
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, nutriscore_grade, stock_quantity, rfid_security_required, ingredients_text, allergen_codes, nutriments, attributes) VALUES
('0680569123565', 'Atlantic salmon',   'Norvik Fjord',          14.99,  'USD', 7, 11, 'b', 100, TRUE,
 'Fresh Atlantic salmon fillet, skin-on.',
 ARRAY['Fish'],
 '{"energy-kcal_100g":208,"sugars_100g":0.0,"fat_100g":13.4,"salt_100g":0.1}'::JSONB,
 '{"unit":"lb","dietary":["Halal","Gluten-free","Low sugar"],"about":"Cold-chain item — RFID tagged. Pick up at the seafood counter on your way out."}'::JSONB);

-- ─── Aisle 12 · Halal counter ────────────────────────────────────────
-- 3456789012 → rfid scan variant (cold-chain meat is RFID-tagged).
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, nutriscore_grade, stock_quantity, rfid_security_required, ingredients_text, allergen_codes, nutriments, attributes) VALUES
('3456789012',    'Chicken breast',    'Halayeb Halal',          8.99,  'USD', 8, 12, 'b', 100, TRUE,
 'Halal-certified chicken breast, boneless and skinless. May contain up to 4% added water.',
 ARRAY[]::TEXT[],
 '{"energy-kcal_100g":165,"sugars_100g":0.0,"fat_100g":3.6,"salt_100g":0.2}'::JSONB,
 '{"unit":"lb","dietary":["Halal","Gluten-free","Low sugar"],"about":"Cold-chain item — RFID tagged. Pick up at the meat counter on your way out."}'::JSONB);

-- Bump the category sequence past the new 6-8 ids we hand-allocated.
SELECT setval('categories_id_seq', 8);
