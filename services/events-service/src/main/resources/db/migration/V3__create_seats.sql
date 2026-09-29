CREATE TABLE seats
(
    id          UUID PRIMARY KEY,
    section_id  UUID NOT NULL,
    row_name    VARCHAR(20) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_seats_section
        FOREIGN KEY (section_id)
            REFERENCES venue_sections(id)
            ON DELETE CASCADE,

    CONSTRAINT uq_seat_position
        UNIQUE (section_id, row_name, seat_number)
);

CREATE INDEX idx_seats_section_id
    ON seats(section_id);