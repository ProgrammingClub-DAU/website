-- What kind of event this is: the badge on the events timeline.
--
-- Nullable, and deliberately without a default. Every event that already exists
-- was created before anyone was asked this question, and defaulting them to
-- 'OTHER' would put a badge reading OTHER on the club's whole history -- worse
-- than no badge at all. An event with no type simply shows none until an admin
-- edits it and says.

ALTER TABLE events ADD COLUMN IF NOT EXISTS event_type VARCHAR(20);

ALTER TABLE events ADD CONSTRAINT events_event_type_check
    CHECK (event_type IS NULL OR event_type IN ('FLAGSHIP', 'CONTEST', 'WORKSHOP', 'ICPC', 'TALK'));
