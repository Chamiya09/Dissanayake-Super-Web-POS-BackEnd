-- V1 created `id` as SERIAL (INTEGER). Hibernate maps Java Long → BIGINT.
-- This migration upgrades the column and its backing sequence to BIGINT.

ALTER TABLE suppliers ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE suppliers_id_seq AS BIGINT;
