CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    full_name VARCHAR(200) NOT NULL,
    role VARCHAR(30) NOT NULL,
    ghana_card_number VARCHAR(255),
    region VARCHAR(100),
    town VARCHAR(100),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    rating_avg DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_orders INTEGER NOT NULL DEFAULT 0,
    profile_image_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE factories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(200) NOT NULL,
    description TEXT,
    machinery_list JSONB,
    min_order_quantity INTEGER,
    max_order_quantity INTEGER,
    gps_coordinates GEOMETRY(POINT, 4326),
    address VARCHAR(500),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verification_notes TEXT,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    featured_until TIMESTAMP WITH TIME ZONE,
    response_time_hours DOUBLE PRECISION,
    completion_rate DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE factory_sectors (
    factory_id UUID NOT NULL REFERENCES factories(id) ON DELETE CASCADE,
    sector_tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (factory_id, sector_tag)
);

CREATE TABLE job_listings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sme_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    product_type VARCHAR(100) NOT NULL,
    sector_tag VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    specifications TEXT,
    budget_min_ghs NUMERIC(14,2),
    budget_max_ghs NUMERIC(14,2),
    deadline DATE NOT NULL,
    delivery_address VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_job_budget CHECK (budget_min_ghs IS NULL OR budget_max_ghs IS NULL OR budget_min_ghs <= budget_max_ghs),
    CONSTRAINT chk_job_quantity CHECK (quantity > 0)
);

CREATE TABLE job_attachments (
    job_id UUID NOT NULL REFERENCES job_listings(id) ON DELETE CASCADE,
    attachment_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (job_id, attachment_url)
);

CREATE TABLE bids (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL REFERENCES job_listings(id) ON DELETE CASCADE,
    factory_id UUID NOT NULL REFERENCES factories(id) ON DELETE CASCADE,
    price_per_unit_ghs NUMERIC(14,2) NOT NULL,
    total_price_ghs NUMERIC(14,2) NOT NULL,
    production_days INTEGER NOT NULL,
    delivery_date_estimate DATE NOT NULL,
    message TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_bid_price CHECK (price_per_unit_ghs > 0 AND total_price_ghs > 0),
    CONSTRAINT chk_bid_days CHECK (production_days > 0),
    CONSTRAINT uq_bid_job_factory UNIQUE (job_id, factory_id)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL UNIQUE REFERENCES job_listings(id) ON DELETE CASCADE,
    bid_id UUID NOT NULL REFERENCES bids(id) ON DELETE CASCADE,
    sme_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    factory_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agreed_amount_ghs NUMERIC(14,2) NOT NULL,
    platform_fee_ghs NUMERIC(14,2) NOT NULL,
    factory_payout_ghs NUMERIC(14,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PAYMENT_PENDING',
    quality_check_deadline TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_order_amount CHECK (agreed_amount_ghs > 0)
);

CREATE TABLE escrow_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    paystack_reference VARCHAR(100) UNIQUE,
    paystack_authorization_code VARCHAR(100),
    amount_ghs NUMERIC(14,2) NOT NULL,
    fee_amount_ghs NUMERIC(14,2) NOT NULL,
    factory_payout_ghs NUMERIC(14,2) NOT NULL,
    payment_method VARCHAR(30),
    escrow_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP WITH TIME ZONE,
    released_at TIMESTAMP WITH TIME ZONE,
    refunded_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewed_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    overall_rating INTEGER NOT NULL,
    quality_rating INTEGER,
    timeliness_rating INTEGER,
    communication_rating INTEGER,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_review_rating CHECK (overall_rating BETWEEN 1 AND 5)
);

CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    raised_by_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_admin_id UUID REFERENCES users(id) ON DELETE SET NULL,
    admin_notes TEXT,
    resolution_amount_ghs NUMERIC(14,2),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE dispute_evidence (
    dispute_id UUID NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    evidence_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (dispute_id, evidence_url)
);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    attachment_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE featured_listings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    factory_id UUID NOT NULL UNIQUE REFERENCES factories(id) ON DELETE CASCADE,
    amount_ghs NUMERIC(14,2) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    paystack_reference VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    action VARCHAR(30) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
