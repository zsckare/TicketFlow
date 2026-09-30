ALTER TABLE issued_tickets
    ADD COLUMN checked_in_by_user_id UUID;

CREATE INDEX idx_issued_tickets_checked_in_by
    ON issued_tickets (checked_in_by_user_id)
    WHERE checked_in_by_user_id IS NOT NULL;
