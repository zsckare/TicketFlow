CREATE TABLE orders
(
    id UUID PRIMARY KEY,

    inventory_id UUID NOT NULL,

    reservation_id UUID,

    amount NUMERIC(12, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    status VARCHAR(30) NOT NULL,

    failure_reason VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_orders_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_orders_status
        CHECK (
            status IN (
                       'PENDING',
                       'RESERVED',
                       'CONFIRMED',
                       'FAILED',
                       'CANCELLED'
                )
            )
);

CREATE INDEX idx_orders_inventory_id
    ON orders(inventory_id);

CREATE INDEX idx_orders_reservation_id
    ON orders(reservation_id);

CREATE INDEX idx_orders_status
    ON orders(status);