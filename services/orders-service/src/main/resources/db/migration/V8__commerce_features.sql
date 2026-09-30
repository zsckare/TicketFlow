ALTER TABLE order_items ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD CONSTRAINT chk_order_item_status CHECK (status IN ('ACTIVE','REFUNDED','CANCELLED'));

ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS promotion_code VARCHAR(64);

CREATE TABLE ticket_transfers (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES issued_tickets(id),
    from_user_id UUID NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transfer_token UUID NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT chk_ticket_transfer_status CHECK(status IN ('PENDING','ACCEPTED','CANCELLED','EXPIRED'))
);
CREATE INDEX idx_ticket_transfers_ticket ON ticket_transfers(ticket_id);
CREATE INDEX idx_ticket_transfers_recipient ON ticket_transfers(lower(recipient_email));

CREATE TABLE ticket_ownership_history (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES issued_tickets(id),
    from_user_id UUID,
    to_user_id UUID NOT NULL,
    reason VARCHAR(30) NOT NULL,
    transfer_id UUID REFERENCES ticket_transfers(id),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ticket_ownership_ticket ON ticket_ownership_history(ticket_id);

CREATE TABLE promotions (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(12,2) NOT NULL,
    event_id UUID,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    max_uses INTEGER,
    max_uses_per_user INTEGER NOT NULL DEFAULT 1,
    current_uses INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_promotion_type CHECK(discount_type IN ('PERCENTAGE','FIXED')),
    CONSTRAINT chk_promotion_value CHECK(discount_value > 0)
);

CREATE TABLE promotion_redemptions (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotions(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    user_id UUID NOT NULL,
    discount_amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(promotion_id, order_id)
);
