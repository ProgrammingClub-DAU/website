-- When Codeforces / LeetCode last said a member's handle does not exist. Null when
-- the handle is fine or has not been checked yet.
--
-- A handle that does not exist used to fail silently: the member got no rating, no
-- solves and no ranklist row, and only an admin could see why, in Sync Health. The
-- member's own profile now says so. Cleared by the next successful sync of that
-- handle, and whenever the member changes it.

ALTER TABLE users ADD COLUMN IF NOT EXISTS cf_handle_invalid_at       TIMESTAMP(6);  -- UTC (D31)
ALTER TABLE users ADD COLUMN IF NOT EXISTS leetcode_handle_invalid_at TIMESTAMP(6);  -- UTC (D31)
