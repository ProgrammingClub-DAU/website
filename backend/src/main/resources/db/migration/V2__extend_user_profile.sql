-- Phase 2, Stage 0: extend `users` with profile, social, and club-position fields.
--
-- Every column here is NULLABLE by design. Phase 1 rows already exist in
-- production, and a NOT NULL column with no default would fail the migration
-- against a populated table. Fields the UI treats as mandatory (phone number)
-- are enforced in the service layer, not the schema -- see the note in
-- Section 1 of documents/phase_2_execution_playbook.md.
--
-- Column lengths must match the `length =` attribute on the corresponding
-- User.java field exactly: `ddl-auto: validate` compares them at startup.

ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url      VARCHAR(512);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number    VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS leetcode_handle VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS leetcode_rating INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS codechef_url    VARCHAR(512);
ALTER TABLE users ADD COLUMN IF NOT EXISTS atcoder_url     VARCHAR(512);
ALTER TABLE users ADD COLUMN IF NOT EXISTS github_url      VARCHAR(512);
ALTER TABLE users ADD COLUMN IF NOT EXISTS linkedin_url    VARCHAR(512);
ALTER TABLE users ADD COLUMN IF NOT EXISTS club_role       VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS batch_year      INTEGER;

-- Mirrors the treatment of codeforces_handle in V1: one member per LeetCode
-- account, so a sync cannot silently attribute one handle's rating to two rows.
-- Added as a named constraint rather than inline UNIQUE so it can be dropped by
-- name if the rule is ever relaxed.
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_leetcode_handle;
ALTER TABLE users ADD CONSTRAINT uk_users_leetcode_handle UNIQUE (leetcode_handle);

-- club_role is the club POSITION (Convenor, Core, ...). It is deliberately
-- separate from `role`, which is the platform PERMISSION (ROLE_USER /
-- ROLE_ADMIN). Conflating them would give a Batch Representative admin API
-- access. NULL means unassigned and renders as "Member" on the frontend;
-- STUDENT is an explicit assignment meaning "member holding no position".
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_club_role_check;
ALTER TABLE users ADD CONSTRAINT users_club_role_check CHECK (club_role IN (
    'CONVENOR',
    'DEPUTY_CONVENOR',
    'CORE',
    'ASSOCIATE_CORE',
    'BATCH_REPRESENTATIVE',
    'EX_PC_MEMBER',
    'EX_CORE',
    'EX_CDC',
    'STUDENT'
));

-- The LeetCode sync looks members up by handle, and the leaderboard filters by
-- club position; both are hot paths. idx_leetcode_handle duplicates the index
-- the UNIQUE constraint creates -- kept for symmetry with idx_codeforces_handle
-- in V1 and because User.java declares it in @Table(indexes = ...).
CREATE INDEX IF NOT EXISTS idx_leetcode_handle ON users (leetcode_handle);
CREATE INDEX IF NOT EXISTS idx_club_role ON users (club_role);
