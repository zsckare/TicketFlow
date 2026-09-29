ALTER TABLE orders
    ADD COLUMN user_id UUID,
    ADD COLUMN payment_id UUID;

CREATE INDEX idx_orders_user_id
    ON orders(user_id);

CREATE UNIQUE INDEX idx_orders_payment_id
    ON orders(payment_id)
    WHERE payment_id IS NOT NULL;
