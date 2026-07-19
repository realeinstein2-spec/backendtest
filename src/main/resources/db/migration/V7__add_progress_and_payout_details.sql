-- 1. Add progress columns to orders
ALTER TABLE orders 
ADD COLUMN current_progress_percentage INTEGER DEFAULT 0 CHECK (current_progress_percentage BETWEEN 0 AND 100),
ADD COLUMN current_production_stage VARCHAR(100) DEFAULT 'Payment Pending';

-- 2. Add Paystack payout routing columns to factories
ALTER TABLE factories
ADD COLUMN payout_account_type VARCHAR(50) CHECK (payout_account_type IN ('mobile_money', 'nuban')),
ADD COLUMN payout_account_name VARCHAR(255),
ADD COLUMN payout_account_number VARCHAR(100),
ADD COLUMN payout_bank_code VARCHAR(50);

-- 3. Create the progress log timeline table
CREATE TABLE order_progress_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    percentage INTEGER NOT NULL CHECK (percentage BETWEEN 0 AND 100),
    stage_name VARCHAR(100) NOT NULL,
    notes VARCHAR(1000),
    photo_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_progress_logs_order ON order_progress_logs(order_id);
