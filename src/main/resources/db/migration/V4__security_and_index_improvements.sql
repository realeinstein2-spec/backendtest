-- V4: Security improvements and missing indexes
-- Applied: 2026-07-15

-- ============================================================
-- OTP Brute-Force Protection: Add attempt_count column
-- ============================================================
ALTER TABLE otp_verifications
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;

-- ============================================================
-- Missing Indexes (identified in performance audit)
-- ============================================================

-- orders.quality_check_deadline — used by OrderCompletionScheduler
CREATE INDEX IF NOT EXISTS idx_orders_quality_check_deadline
    ON orders (quality_check_deadline)
    WHERE deleted_at IS NULL;

-- orders.status — used in many query filters
CREATE INDEX IF NOT EXISTS idx_orders_status
    ON orders (status)
    WHERE deleted_at IS NULL;

-- orders.factory_id — used for factory order listing
CREATE INDEX IF NOT EXISTS idx_orders_factory_id
    ON orders (factory_id)
    WHERE deleted_at IS NULL;

-- otp_verifications.expiry_time — used for expiry checks
CREATE INDEX IF NOT EXISTS idx_otp_expiry
    ON otp_verifications (expiry_time);

-- featured_listings.ends_at — used for expiry queries
CREATE INDEX IF NOT EXISTS idx_featured_ends_at
    ON featured_listings (ends_at);

-- escrow_transactions.order_id — used for escrow lookups by order
CREATE INDEX IF NOT EXISTS idx_escrow_order_id
    ON escrow_transactions (order_id);

-- escrow_transactions.paystack_reference — used for webhook processing
CREATE INDEX IF NOT EXISTS idx_escrow_paystack_ref
    ON escrow_transactions (paystack_reference);

-- bids.job_id — used for listing bids by job
CREATE INDEX IF NOT EXISTS idx_bids_job_listing
    ON bids (job_id)
    WHERE deleted_at IS NULL;

-- disputes.order_id — used for order-based dispute lookup
CREATE INDEX IF NOT EXISTS idx_disputes_order_id
    ON disputes (order_id);

-- ============================================================
-- Add partial unique index on featured_listings (active only)
-- Allows re-featuring after a listing expires
-- ============================================================
-- Drop old plain unique constraint if it exists
ALTER TABLE featured_listings
    DROP CONSTRAINT IF EXISTS featured_listings_factory_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS idx_featured_active_unique
    ON featured_listings (factory_id)
    WHERE is_active = TRUE;
