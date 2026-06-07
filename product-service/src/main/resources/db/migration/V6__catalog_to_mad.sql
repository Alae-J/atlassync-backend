-- Convert the phygital catalog from USD to MAD. The store is in Morocco,
-- so price tags read in dirhams. Prices are *re-priced*, not just
-- converted — these are realistic Moroccan supermarket prices, not USD
-- × 10. Anything we pull lazily from Open Food Facts later gets a
-- MAD-by-aisle policy applied in ProductEnricher, so this migration
-- only touches the existing seed.

-- Produce — aisle 1
UPDATE products SET price =  10.00, currency_code = 'MAD' WHERE barcode = '1234567890';     -- Bananas
UPDATE products SET price =  18.00, currency_code = 'MAD' WHERE barcode = '0680569123459';  -- Avocado
UPDATE products SET price =   8.00, currency_code = 'MAD' WHERE barcode = '0680569123466';  -- Roma tomatoes
UPDATE products SET price =   5.00, currency_code = 'MAD' WHERE barcode = '0680569123473';  -- Yellow onions
UPDATE products SET price =  22.00, currency_code = 'MAD' WHERE barcode = '0680569123480';  -- Honeycrisp apples

-- Dairy — aisle 5
UPDATE products SET price =   8.50, currency_code = 'MAD' WHERE barcode = '2345678901';     -- Whole milk (allergen-locked scan variant)
UPDATE products SET price =  28.00, currency_code = 'MAD' WHERE barcode = '0680569123497';  -- Eggs, dozen
UPDATE products SET price =  72.00, currency_code = 'MAD' WHERE barcode = '0680569123503';  -- Aged cheddar (out of stock)
UPDATE products SET price =  24.00, currency_code = 'MAD' WHERE barcode = '0680569123510';  -- Greek yogurt

-- Bakery — aisle 8
UPDATE products SET price =  18.00, currency_code = 'MAD' WHERE barcode = '0680569123527';  -- Sourdough loaf

-- Pantry — aisle 9
UPDATE products SET price =  12.00, currency_code = 'MAD' WHERE barcode = '0680569123534';  -- Penne pasta
UPDATE products SET price = 110.00, currency_code = 'MAD' WHERE barcode = '0680569123541';  -- Olive oil
UPDATE products SET price = 130.00, currency_code = 'MAD' WHERE barcode = '0680569123558';  -- Whole-bean coffee

-- Seafood — aisle 11
UPDATE products SET price = 180.00, currency_code = 'MAD' WHERE barcode = '0680569123565';  -- Atlantic salmon (RFID)

-- Halal counter — aisle 12
UPDATE products SET price =  65.00, currency_code = 'MAD' WHERE barcode = '3456789012';     -- Chicken breast (RFID)

-- Anything else still in USD from older seeds (V2) gets bulk-migrated.
UPDATE products SET currency_code = 'MAD' WHERE currency_code = 'USD';
