CREATE TABLE venue_sections
(
    id          UUID PRIMARY KEY,
    venue_id    UUID NOT NULL,
    name        VARCHAR(150) NOT NULL,
    type        VARCHAR(30) NOT NULL,
    capacity    INTEGER NOT NULL,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_venue_sections_venue
        FOREIGN KEY (venue_id)
            REFERENCES venues(id)
            ON DELETE CASCADE,

    CONSTRAINT chk_venue_sections_type
        CHECK (type IN ('GENERAL_ADMISSION', 'RESERVED_SEATING')),

    CONSTRAINT chk_venue_sections_capacity
        CHECK (capacity > 0)
);

CREATE INDEX idx_venue_sections_venue_id
    ON venue_sections(venue_id);