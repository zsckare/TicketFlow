CREATE TABLE pricing_tiers (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    section_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    sales_start_at TIMESTAMPTZ,
    sales_end_at TIMESTAMPTZ,
    priority INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_pricing_tier_price CHECK(price >= 0)
);
CREATE INDEX idx_pricing_tiers_event_section ON pricing_tiers(event_id, section_id);
