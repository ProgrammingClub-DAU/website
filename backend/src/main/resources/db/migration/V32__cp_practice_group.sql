-- The CP Practice Group: students the committee follows closely. Only admins see
-- the group; a member sees just their own membership, on their own profile.
-- batch_year is the group's batch, set by an admin (2029, 2030, ...), separate
-- from the batch on the student's profile.

CREATE TABLE IF NOT EXISTS cp_group_members (
    user_id          BIGINT       NOT NULL,
    batch_year       INTEGER      NOT NULL,
    added_at         TIMESTAMP(6) NOT NULL,  -- UTC (D31)
    added_by         BIGINT,
    notes            TEXT,                   -- admins' private notes on the student
    notes_updated_at TIMESTAMP(6),           -- UTC (D31)
    notes_updated_by BIGINT,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_cp_group_user     FOREIGN KEY (user_id)          REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_group_added_by FOREIGN KEY (added_by)         REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_cp_group_notes_by FOREIGN KEY (notes_updated_by) REFERENCES users (id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_cp_group_batch ON cp_group_members (batch_year);
