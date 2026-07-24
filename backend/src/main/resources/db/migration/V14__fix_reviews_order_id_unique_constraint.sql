-- Flyway Migration V14: Remove inline UNIQUE constraint on reviews.order_id
-- This allows both SME and Factory to submit reviews for the same order.

ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_order_id_key;
