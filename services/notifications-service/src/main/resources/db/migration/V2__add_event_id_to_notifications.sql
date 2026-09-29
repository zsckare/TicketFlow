ALTER TABLE notifications ADD COLUMN event_id UUID NULL;
CREATE UNIQUE INDEX ux_notifications_event_id ON notifications(event_id) WHERE event_id IS NOT NULL;
