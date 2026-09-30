CREATE TABLE issued_tickets (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    order_item_id UUID NOT NULL REFERENCES order_items(id),
    user_id UUID NOT NULL,
    event_id UUID NOT NULL,
    inventory_id UUID NOT NULL,
    section_id UUID,
    seat_id UUID,
    admission_token UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    checked_in_at TIMESTAMPTZ,
    CONSTRAINT chk_issued_ticket_status CHECK(status IN ('ISSUED','USED','CANCELLED'))
);
CREATE UNIQUE INDEX idx_issued_ticket_item ON issued_tickets(order_item_id);
CREATE INDEX idx_issued_ticket_user ON issued_tickets(user_id);
CREATE INDEX idx_issued_ticket_event ON issued_tickets(event_id);
