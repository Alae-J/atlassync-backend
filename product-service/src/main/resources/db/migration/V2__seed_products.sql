-- Categories
INSERT INTO categories (id, name, slug, parent_id) VALUES
(1, 'Produce',        'produce',        NULL),
(2, 'Dairy',          'dairy',          NULL),
(3, 'Pasta & Sauces', 'pasta-sauces',   NULL),
(4, 'Drinks',         'drinks',         NULL),
(5, 'Snacks',         'snacks',         NULL);

-- Aisle 1 - Produce
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, image_url, nutriscore_grade, stock_quantity, rfid_security_required, nutriments, attributes) VALUES
('3013921052007', 'Bananes',              'Cavendish',     1.2900, 'EUR', 1, 1, 'https://images.openfoodfacts.org/images/products/301/392/105/2007/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":89,"fat":0.3,"carbohydrates":22.8,"proteins":1.1,"salt":0.01}',  '{"organic":false,"origin":"Ecuador"}'),
('8410700011244', 'Pommes Golden',        'Marlene',       2.4900, 'EUR', 1, 1, 'https://images.openfoodfacts.org/images/products/841/070/001/1244/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":52,"fat":0.2,"carbohydrates":13.8,"proteins":0.3,"salt":0.0}',   '{"organic":false,"origin":"Italy"}'),
('3560070825639', 'Tomates Rondes',       'Carrefour',     2.9900, 'EUR', 1, 1, 'https://images.openfoodfacts.org/images/products/356/007/082/5639/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":18,"fat":0.2,"carbohydrates":3.9,"proteins":0.9,"salt":0.01}',  '{"organic":false,"origin":"France"}'),
('4056489521839', 'Avocats',              'Nature',        3.9900, 'EUR', 1, 1, 'https://images.openfoodfacts.org/images/products/405/648/952/1839/front_fr.jpg',  'c', 100, FALSE, '{"energy_kcal":160,"fat":14.7,"carbohydrates":8.5,"proteins":2.0,"salt":0.01}', '{"organic":false,"origin":"Peru"}');

-- Aisle 2 - Dairy
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, image_url, nutriscore_grade, stock_quantity, rfid_security_required, nutriments, attributes) VALUES
('3250390003700', 'Lait Demi-Ecreme',     'Lactel',        1.0500, 'EUR', 2, 2, 'https://images.openfoodfacts.org/images/products/325/039/000/3700/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":46,"fat":1.5,"carbohydrates":4.8,"proteins":3.2,"salt":0.1}',   '{"lactose_free":false,"uht":true}'),
('3033490004750', 'Yaourt Nature',        'Danone',        2.1500, 'EUR', 2, 2, 'https://images.openfoodfacts.org/images/products/303/349/000/4750/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":54,"fat":1.2,"carbohydrates":6.3,"proteins":4.7,"salt":0.15}',  '{"pack_size":"4x125g","type":"plain"}'),
('3175680011480', 'Beurre Doux',          'President',     2.7900, 'EUR', 2, 2, 'https://images.openfoodfacts.org/images/products/317/568/001/1480/front_fr.jpg',  'd', 100, FALSE, '{"energy_kcal":741,"fat":82.0,"carbohydrates":0.6,"proteins":0.7,"salt":0.04}', '{"salted":false,"fat_content":"82%"}'),
('3073781069563', 'Emmental Rape',        'President',     3.2900, 'EUR', 2, 2, 'https://images.openfoodfacts.org/images/products/307/378/106/9563/front_fr.jpg',  'd', 100, FALSE, '{"energy_kcal":380,"fat":29.0,"carbohydrates":0.5,"proteins":28.0,"salt":0.8}', '{"grated":true,"type":"emmental"}');

-- Aisle 3 - Pasta & Sauces
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, image_url, nutriscore_grade, stock_quantity, rfid_security_required, nutriments, attributes) VALUES
('8076802085738', 'Spaghetti N.5',        'Barilla',       1.5500, 'EUR', 3, 3, 'https://images.openfoodfacts.org/images/products/807/680/208/5738/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":359,"fat":1.5,"carbohydrates":72.0,"proteins":12.5,"salt":0.0}', '{"cooking_time":"8min","weight":"500g"}'),
('8076809513753', 'Penne Rigate',         'Barilla',       1.5500, 'EUR', 3, 3, 'https://images.openfoodfacts.org/images/products/807/680/951/3753/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":359,"fat":1.5,"carbohydrates":72.0,"proteins":12.5,"salt":0.0}', '{"cooking_time":"11min","weight":"500g"}'),
('8005110070402', 'Passata di Pomodoro',  'Mutti',         1.9900, 'EUR', 3, 3, 'https://images.openfoodfacts.org/images/products/800/511/007/0402/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":26,"fat":0.1,"carbohydrates":4.5,"proteins":1.3,"salt":0.05}',  '{"type":"passata","weight":"700g"}'),
('8076809545693', 'Pesto alla Genovese',  'Barilla',       2.9900, 'EUR', 3, 3, 'https://images.openfoodfacts.org/images/products/807/680/954/5693/front_fr.jpg',  'c', 100, FALSE, '{"energy_kcal":396,"fat":37.0,"carbohydrates":7.5,"proteins":5.5,"salt":2.6}', '{"type":"pesto","weight":"190g"}');

-- Aisle 4 - Drinks
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, image_url, nutriscore_grade, stock_quantity, rfid_security_required, nutriments, attributes) VALUES
('3051580291204', 'Jus d''Orange',        'Tropicana',     3.4900, 'EUR', 4, 4, 'https://images.openfoodfacts.org/images/products/305/158/029/1204/front_fr.jpg',  'c', 100, FALSE, '{"energy_kcal":43,"fat":0.1,"carbohydrates":10.0,"proteins":0.6,"salt":0.01}', '{"volume":"1L","type":"pure_juice"}'),
('5449000000996', 'Coca-Cola',            'Coca-Cola',     1.9900, 'EUR', 4, 4, 'https://images.openfoodfacts.org/images/products/544/900/000/0996/front_fr.jpg',  'e', 100, FALSE, '{"energy_kcal":42,"fat":0.0,"carbohydrates":10.6,"proteins":0.0,"salt":0.0}',  '{"volume":"1.5L","type":"soda"}'),
('3068320114019', 'Eau Minerale Naturelle','Evian',         0.8900, 'EUR', 4, 4, 'https://images.openfoodfacts.org/images/products/306/832/011/4019/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":0,"fat":0.0,"carbohydrates":0.0,"proteins":0.0,"salt":0.0}',    '{"volume":"1.5L","type":"mineral_water"}'),
('3500610033629', 'Vin Rouge Bordeaux',   'Baron de Lestac', 5.9900, 'EUR', 4, 4, 'https://images.openfoodfacts.org/images/products/350/061/003/3629/front_fr.jpg', NULL, 100, TRUE,  '{"energy_kcal":70,"fat":0.0,"carbohydrates":0.3,"proteins":0.1,"salt":0.0}',  '{"volume":"750ml","type":"red_wine","appellation":"Bordeaux","alcohol":"13.5%"}');

-- Aisle 5 - Snacks
INSERT INTO products (barcode, name, brand, price, currency_code, category_id, aisle_number, image_url, nutriscore_grade, stock_quantity, rfid_security_required, nutriments, attributes) VALUES
('5053990101573', 'Pringles Original',    'Pringles',      2.4900, 'EUR', 5, 5, 'https://images.openfoodfacts.org/images/products/505/399/010/1573/front_fr.jpg',  'c', 100, FALSE, '{"energy_kcal":520,"fat":32.0,"carbohydrates":51.0,"proteins":4.5,"salt":1.3}', '{"weight":"175g","type":"chips"}'),
('7622210449283', 'Chocolat au Lait',     'Milka',         1.8900, 'EUR', 5, 5, 'https://images.openfoodfacts.org/images/products/762/221/044/9283/front_fr.jpg',  'd', 100, FALSE, '{"energy_kcal":530,"fat":29.0,"carbohydrates":58.0,"proteins":6.9,"salt":0.4}', '{"weight":"100g","type":"milk_chocolate"}'),
('7622210988485', 'Petit Beurre',         'LU',            1.5900, 'EUR', 5, 5, 'https://images.openfoodfacts.org/images/products/762/221/098/8485/front_fr.jpg',  'a', 100, FALSE, '{"energy_kcal":435,"fat":12.0,"carbohydrates":75.0,"proteins":7.5,"salt":1.0}', '{"weight":"200g","type":"biscuit"}'),
('8410076600608', 'Crunchy Granola Bars', 'Nature Valley', 3.2900, 'EUR', 5, 5, 'https://images.openfoodfacts.org/images/products/841/007/660/0608/front_fr.jpg',  'c', 100, FALSE, '{"energy_kcal":471,"fat":18.0,"carbohydrates":64.0,"proteins":7.5,"salt":0.6}', '{"weight":"210g","type":"granola_bar"}');

-- Reset category sequence
SELECT setval('categories_id_seq', 5);
