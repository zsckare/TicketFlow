CREATE TABLE events
(
    id          UUID PRIMARY KEY,
    venue_id    UUID NOT NULL,

    name        VARCHAR(200) NOT NULL,
    description TEXT,

    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ,

    status      VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_events_venue
        FOREIGN KEY (venue_id)
            REFERENCES venues(id),

    CONSTRAINT chk_events_status
        CHECK (
            status IN (
                       'DRAFT',
                       'PUBLISHED',
                       'CANCELLED',
                       'FINISHED'
                )
            ),

    CONSTRAINT chk_events_dates
        CHECK (
            ends_at IS NULL
                OR ends_at > starts_at
            )
);

CREATE INDEX idx_events_venue_id
    ON events(venue_id);

CREATE INDEX idx_events_starts_at
    ON events(starts_at);

CREATE INDEX idx_events_status
    ON events(status);