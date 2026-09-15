-- Which stage of the course a member is in, chosen once at registration.
--
-- Two values, not a batch year: the club only ever asks the question to tell
-- first-years from everyone else, and a first-year in 2026 is a second-year in
-- 2027, which a stored year would silently get wrong.
--
-- Nullable, because every account created before this migration answered no such
-- question. Those members are prompted by the profile completeness check instead
-- of being given a guessed value.

ALTER TABLE users ADD COLUMN IF NOT EXISTS academic_year VARCHAR(30);

ALTER TABLE users ADD CONSTRAINT users_academic_year_check
    CHECK (academic_year IS NULL OR academic_year IN ('FIRST_YEAR', 'SECOND_YEAR_ONWARDS'));
