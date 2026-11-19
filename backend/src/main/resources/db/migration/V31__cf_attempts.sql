-- Problems a member tried on Codeforces and has not solved (yet): the most recent
-- failed submission per problem. Practice suggests these again ("try again").
-- A row stays after the problem is solved; readers exclude solved problems.

CREATE TABLE IF NOT EXISTS cf_attempts (
    user_id            BIGINT       NOT NULL,
    problem_id         BIGINT       NOT NULL,
    last_attempt_at    TIMESTAMP(6) NOT NULL,  -- UTC (D31)
    last_submission_id BIGINT       NOT NULL,
    last_verdict       VARCHAR(40)  NOT NULL,  -- WRONG_ANSWER, TIME_LIMIT_EXCEEDED, ...
    PRIMARY KEY (user_id, problem_id),
    CONSTRAINT fk_cf_attempts_user    FOREIGN KEY (user_id)    REFERENCES users (id)       ON DELETE CASCADE,
    CONSTRAINT fk_cf_attempts_problem FOREIGN KEY (problem_id) REFERENCES cf_problems (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_cf_attempts_user_time ON cf_attempts (user_id, last_attempt_at);

-- Failed submissions were never stored, so every member's history is read once
-- more from the start: clearing the cursor makes the next sync a full backfill.
-- Solves are upserted keeping the earliest accepted submission, so nothing is
-- duplicated or lost.
UPDATE users SET cf_last_submission_id = NULL WHERE cf_last_submission_id IS NOT NULL;
