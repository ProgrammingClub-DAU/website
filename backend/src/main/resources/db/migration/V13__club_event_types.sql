-- The club's own event types, replacing the five placeholders from V11.
--
-- The new names come from the club's orientation deck. POST_CONTEST_DISCUSSION
-- is 23 characters, so the column is widened first.
--
-- Existing rows are mapped where the meaning carries over: a flagship contest is
-- the IPC, and a workshop or a talk is a lecture. CONTEST and ICPC have no
-- honest equivalent, so those rows lose their badge (NULL) until an admin picks
-- one -- the same state every event was in before V11.

ALTER TABLE events DROP CONSTRAINT IF EXISTS events_event_type_check;

ALTER TABLE events ALTER COLUMN event_type TYPE VARCHAR(40);

UPDATE events
SET event_type = CASE event_type
    WHEN 'FLAGSHIP' THEN 'IPC'
    WHEN 'WORKSHOP' THEN 'LECTURE'
    WHEN 'TALK' THEN 'LECTURE'
    ELSE NULL
END
WHERE event_type IS NOT NULL;

ALTER TABLE events ADD CONSTRAINT events_event_type_check
    CHECK (event_type IS NULL OR event_type IN (
        'IPC', 'JUNIORS_CONTEST', 'INTER_WING', 'ROUND_ROBIN_RELAY',
        'LECTURE', 'POST_CONTEST_DISCUSSION'
    ));
