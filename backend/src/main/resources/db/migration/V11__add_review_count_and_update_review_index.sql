ALTER TABLE users ADD COLUMN review_count INT NOT NULL DEFAULT 0;

ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_order_id_key;
DROP INDEX IF EXISTS idx_reviews_order;
CREATE UNIQUE INDEX IF NOT EXISTS idx_reviews_order_reviewer ON reviews(order_id, reviewer_id);
