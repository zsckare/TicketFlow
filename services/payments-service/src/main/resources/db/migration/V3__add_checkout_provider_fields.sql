ALTER TABLE payments ADD COLUMN provider VARCHAR(30) NOT NULL DEFAULT 'SIMULATED';
ALTER TABLE payments ADD COLUMN provider_session_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN checkout_url TEXT;
ALTER TABLE payments ADD COLUMN customer_email VARCHAR(320);
CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_provider_session ON payments(provider_session_id) WHERE provider_session_id IS NOT NULL;
