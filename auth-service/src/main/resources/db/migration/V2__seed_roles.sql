INSERT INTO roles (name) VALUES ('ROLE_CUSTOMER'), ('ROLE_WORKER'), ('ROLE_ADMIN');

INSERT INTO privileges (name) VALUES
    ('SCAN_PRODUCT'), ('VIEW_CART'), ('CHECKOUT'),
    ('VIEW_HELP_REQUESTS'), ('CONFIRM_PRICE'), ('MANAGE_STOCK'),
    ('MANAGE_USERS'), ('MANAGE_CATALOG'), ('VIEW_ANALYTICS');

INSERT INTO roles_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r, privileges p
WHERE r.name = 'ROLE_CUSTOMER' AND p.name IN ('SCAN_PRODUCT', 'VIEW_CART', 'CHECKOUT');

INSERT INTO roles_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r, privileges p
WHERE r.name = 'ROLE_WORKER' AND p.name IN ('VIEW_HELP_REQUESTS', 'CONFIRM_PRICE', 'MANAGE_STOCK');

INSERT INTO roles_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r, privileges p WHERE r.name = 'ROLE_ADMIN';
