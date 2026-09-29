CREATE TABLE ticket_inventory
(
    id UUID PRIMARY KEY,

    event_id UUID NOT NULL,
    seat_id UUID NOT NULL,

    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_ticket_inventory_event_seat
        UNIQUE (event_id, seat_id),

    CONSTRAINT chk_ticket_inventory_price
        CHECK (price >= 0),

    CONSTRAINT chk_ticket_inventory_status
        CHECK (
            status IN (
                       'AVAILABLE',
                       'RESERVED',
                       'SOLD'
                )
            )
);

CREATE INDEX idx_ticket_inventory_event_id
    ON ticket_inventory(event_id);

CREATE INDEX idx_ticket_inventory_status
    ON ticket_inventory(status);

CREATE INDEX idx_ticket_inventory_event_status
    ON ticket_inventory(event_id, status);