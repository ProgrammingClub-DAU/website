-- Bug #4 fix: unique constraint to prevent duplicate solve entries under concurrent polling
ALTER TABLE compete_solve_logs ADD CONSTRAINT uq_solve_log_match_problem UNIQUE (match_id, contest_id, index);
