-- Keep the products id sequence aligned with the latest product id already in DB.
-- New product IDs then continue one-by-one: max(id)+1 -> PI0001 style SKU.

SELECT setval(
    pg_get_serial_sequence('products', 'id'),
    COALESCE((SELECT MAX(id) FROM products), 0) + 1,
    false
);
