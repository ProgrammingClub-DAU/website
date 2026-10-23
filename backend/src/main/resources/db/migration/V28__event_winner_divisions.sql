-- Separate podiums for first-year students and for second year onwards. The club
-- awards a prize in each, so an event such as an IPC can now record a 1st-3rd in
-- each division. Every existing placing is a single overall podium.

ALTER TABLE event_winners ADD COLUMN IF NOT EXISTS category VARCHAR(30) NOT NULL DEFAULT 'OVERALL';

ALTER TABLE event_winners ADD CONSTRAINT event_winners_category_check
    CHECK (category IN ('OVERALL', 'FIRST_YEAR', 'SECOND_YEAR_ONWARDS'));

-- One member per placing *within a division*. A member still holds at most one
-- placing per event (event_winners_unique_member is unchanged).
ALTER TABLE event_winners DROP CONSTRAINT IF EXISTS event_winners_unique_position;
ALTER TABLE event_winners ADD CONSTRAINT event_winners_unique_position UNIQUE (event_id, category, position);

DROP INDEX IF EXISTS idx_event_winners_event;
CREATE INDEX IF NOT EXISTS idx_event_winners_event ON event_winners (event_id, category, position);
