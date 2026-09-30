ALTER TABLE payments ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payment_status;
ALTER TABLE payments ADD CONSTRAINT ck_payment_status CHECK(status IN ('PENDING','SUCCEEDED','FAILED','PARTIALLY_REFUNDED','REFUNDED'));
CREATE TABLE payment_refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    amount NUMERIC(12,2) NOT NULL,
    reason VARCHAR(255),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_refund_amount CHECK(amount > 0)
);
