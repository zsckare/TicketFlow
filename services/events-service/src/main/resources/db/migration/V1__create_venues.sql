CREATE TABLE venues
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    address     VARCHAR(300) NOT NULL,
    city        VARCHAR(150) NOT NULL,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_venues_city
    ON venues(city);