CREATE TABLE event_section_inventory
(
    event_id UUID NOT NULL,
    section_id UUID NOT NULL,
    section_type VARCHAR(30) NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    capacity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, section_id),
    CONSTRAINT chk_event_section_inventory_type CHECK (section_type IN ('GENERAL_ADMISSION', 'RESERVED_SEATING')),
    CONSTRAINT chk_event_section_inventory_price CHECK (base_price >= 0),
    CONSTRAINT chk_event_section_inventory_capacity CHECK (capacity >= 0)
);

ALTER TABLE ticket_inventory
    ADD COLUMN section_id UUID,
    ADD COLUMN price_override NUMERIC(12, 2);

ALTER TABLE ticket_inventory
    ALTER COLUMN seat_id DROP NOT NULL;

ALTER TABLE ticket_inventory
    DROP CONSTRAINT IF EXISTS uq_ticket_inventory_event_seat;

CREATE UNIQUE INDEX uq_ticket_inventory_event_seat
    ON ticket_inventory(event_id, seat_id)
    WHERE seat_id IS NOT NULL;

CREATE INDEX idx_ticket_inventory_event_section
    ON ticket_inventory(event_id, section_id);

ALTER TABLE ticket_inventory
    ADD CONSTRAINT chk_ticket_inventory_price_override
        CHECK (price_override IS NULL OR price_override >= 0);
