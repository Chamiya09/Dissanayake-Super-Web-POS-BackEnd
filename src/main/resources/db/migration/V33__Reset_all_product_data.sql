-- User-requested reset of all product records.
-- Keeps schema, removes data, and restarts product id sequence.

DELETE FROM products;
SELECT setval(pg_get_serial_sequence('products', 'id'), 1, false);
