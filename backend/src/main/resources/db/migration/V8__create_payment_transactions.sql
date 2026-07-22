CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference VARCHAR(100) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount_pesewas BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'GHS',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    authorization_url VARCHAR(1000) NOT NULL,
    paystack_transaction_id VARCHAR(100),
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_transactions_ref ON payment_transactions(reference);
CREATE INDEX idx_payment_transactions_order ON payment_transactions(order_id);
