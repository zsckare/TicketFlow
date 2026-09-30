-- PaymentStatus already includes REFUNDED; this migration documents the transition and adds lookup support.
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
