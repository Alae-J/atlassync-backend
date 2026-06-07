-- Drop the leftover V2 French-language EUR seed. V5's phygital catalog
-- superseded it but never removed the old rows, so the catalog still
-- shows duplicate Lait / Pâtes / Yaourt entries in EUR alongside the
-- MAD-priced phygital ones. Demo-time noise — kill it.

DELETE FROM products WHERE currency_code = 'EUR';
