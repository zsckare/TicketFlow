ALTER TABLE ticket_inventory
    ADD COLUMN reservation_id UUID,
    ADD COLUMN reserved_until TIMESTAMPTZ;

CREATE INDEX idx_ticket_inventory_reservation_id
    ON ticket_inventory(reservation_id);

CREATE INDEX idx_ticket_inventory_reserved_until
    ON ticket_inventory(reserved_until);