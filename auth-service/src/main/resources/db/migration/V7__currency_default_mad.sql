-- Flip the default currency from USD to MAD now that the project is
-- explicitly Moroccan. Existing rows that still carry the old USD default
-- get migrated; rows where a user has actively picked a non-USD currency
-- (EUR etc.) stay as-is.

ALTER TABLE users
    ALTER COLUMN currency_code SET DEFAULT 'MAD';

UPDATE users SET currency_code = 'MAD' WHERE currency_code = 'USD';
