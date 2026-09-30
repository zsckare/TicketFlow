CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    inventory_id UUID NOT NULL,
    reservation_id UUID,
    event_id UUID NOT NULL,
    section_id UUID,
    seat_id UUID,
    unit_price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_item_price CHECK (unit_price >= 0)
);
CREATE INDEX idx_order_items_inventory
    ON order_items (inventory_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- Existing orders remain readable and are backfilled into the new item model.
INSERT INTO order_items(id, order_id, inventory_id, reservation_id, event_id, section_id, seat_id, unit_price, currency, created_at)
SELECT gen_random_uuid(), id, inventory_id, reservation_id, '00000000-0000-0000-0000-000000000000'::uuid, NULL, NULL, amount, currency, created_at
FROM orders;
