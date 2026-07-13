CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_region ON users(region);

CREATE INDEX IF NOT EXISTS idx_factories_user ON factories(user_id);
CREATE INDEX IF NOT EXISTS idx_factories_status ON factories(verification_status);
CREATE INDEX IF NOT EXISTS idx_factories_featured ON factories(is_featured);
CREATE INDEX IF NOT EXISTS idx_factories_location ON factories USING GIST(gps_coordinates);

CREATE INDEX IF NOT EXISTS idx_jobs_sme ON job_listings(sme_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON job_listings(status);
CREATE INDEX IF NOT EXISTS idx_jobs_sector ON job_listings(sector_tag);
CREATE INDEX IF NOT EXISTS idx_jobs_deadline ON job_listings(deadline);

CREATE INDEX IF NOT EXISTS idx_bids_job ON bids(job_id);
CREATE INDEX IF NOT EXISTS idx_bids_factory ON bids(factory_id);
CREATE INDEX IF NOT EXISTS idx_bids_status ON bids(status);

CREATE INDEX IF NOT EXISTS idx_orders_job ON orders(job_id);
CREATE INDEX IF NOT EXISTS idx_orders_bid ON orders(bid_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_sme ON orders(sme_id);
CREATE INDEX IF NOT EXISTS idx_orders_factory ON orders(factory_id);

CREATE INDEX IF NOT EXISTS idx_escrow_order ON escrow_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_escrow_reference ON escrow_transactions(paystack_reference);
CREATE INDEX IF NOT EXISTS idx_escrow_status ON escrow_transactions(escrow_status);

CREATE INDEX IF NOT EXISTS idx_reviews_order ON reviews(order_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewer ON reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewed ON reviews(reviewed_id);

CREATE INDEX IF NOT EXISTS idx_disputes_order ON disputes(order_id);
CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes(status);
CREATE INDEX IF NOT EXISTS idx_disputes_assigned ON disputes(assigned_admin_id);

CREATE INDEX IF NOT EXISTS idx_messages_order ON messages(order_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at);

CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);

CREATE INDEX IF NOT EXISTS idx_featured_factory ON featured_listings(factory_id);
CREATE INDEX IF NOT EXISTS idx_featured_active ON featured_listings(is_active);
