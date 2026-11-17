-- Upsolves read from a club contest's own standings (P1, D12).
--
-- Upsolves used to come only from each member's public submission history
-- (user.status). Codeforces leaves submissions in a *private* gym out of that, so
-- an IPC run in a private gym never showed an upsolve. The standings of a gym
-- contest, read with the club's API key, include each member's PRACTICE and
-- VIRTUAL rows -- every one of which is, by definition, after the contest ended.
-- One row per member per problem they solved that way; rebuilt on every sync.

CREATE TABLE IF NOT EXISTS club_contest_upsolves (
    club_contest_id BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    problem_index   VARCHAR(10) NOT NULL,  -- as club_contest_problems.problem_index
    PRIMARY KEY (club_contest_id, user_id, problem_index),
    CONSTRAINT fk_ccu_contest FOREIGN KEY (club_contest_id) REFERENCES club_contests (id) ON DELETE CASCADE,
    CONSTRAINT fk_ccu_user    FOREIGN KEY (user_id)         REFERENCES users (id)         ON DELETE CASCADE
);
