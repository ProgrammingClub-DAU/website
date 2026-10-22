-- Members pasted profile links into the handle fields, e.g.
-- 'https://codeforces.com/profile/Ishaan-K'. Every platform call rejected them, so
-- those members had no rating, solves or contest history. PlatformHandles now
-- refuses such input on save; this repairs what was saved before.
--
-- Each update skips a row whose cleaned handle already belongs to someone else
-- (case-insensitively), rather than failing the migration on the unique constraint.
-- Such a row is left for the member to fix from their profile.

-- 1. Surrounding whitespace and a leading '@'.
UPDATE users u
SET codeforces_handle = regexp_replace(btrim(u.codeforces_handle), '^@+', '')
WHERE u.codeforces_handle IS NOT NULL
  AND u.codeforces_handle <> regexp_replace(btrim(u.codeforces_handle), '^@+', '')
  AND NOT EXISTS (
      SELECT 1 FROM users o
      WHERE o.id <> u.id
        AND lower(o.codeforces_handle) = lower(regexp_replace(btrim(u.codeforces_handle), '^@+', '')));

UPDATE users u
SET leetcode_handle = regexp_replace(btrim(u.leetcode_handle), '^@+', '')
WHERE u.leetcode_handle IS NOT NULL
  AND u.leetcode_handle <> regexp_replace(btrim(u.leetcode_handle), '^@+', '')
  AND NOT EXISTS (
      SELECT 1 FROM users o
      WHERE o.id <> u.id
        AND lower(o.leetcode_handle) = lower(regexp_replace(btrim(u.leetcode_handle), '^@+', '')));

-- 2. Codeforces profile links: codeforces.com/profile/<handle>[/...][?...]
UPDATE users u
SET codeforces_handle = substring(u.codeforces_handle FROM '(?i)codeforces\.[a-z]+/(?:profile|submissions)/([^/?#[:space:]]+)'),
    rating = NULL,
    max_rating = NULL,
    cf_last_submission_id = NULL
WHERE u.codeforces_handle ~* 'codeforces\.[a-z]+/(profile|submissions)/[^/?#[:space:]]+'
  AND NOT EXISTS (
      SELECT 1 FROM users o
      WHERE o.id <> u.id
        AND lower(o.codeforces_handle) = lower(substring(u.codeforces_handle FROM '(?i)codeforces\.[a-z]+/(?:profile|submissions)/([^/?#[:space:]]+)')));

-- 3. LeetCode profile links: leetcode.com/u/<user>/ or leetcode.com/<user>/
UPDATE users u
SET leetcode_handle = substring(u.leetcode_handle FROM '(?i)leetcode\.[a-z]+/(?:u/)?([^/?#[:space:]]+)'),
    leetcode_rating = NULL,
    leetcode_total_solved = NULL,
    leetcode_easy_solved = NULL,
    leetcode_medium_solved = NULL,
    leetcode_hard_solved = NULL
WHERE u.leetcode_handle ~* 'leetcode\.[a-z]+/(u/)?[^/?#[:space:]]+'
  AND lower(substring(u.leetcode_handle FROM '(?i)leetcode\.[a-z]+/(?:u/)?([^/?#[:space:]]+)'))
      NOT IN ('problems', 'problemset', 'contest', 'discuss', 'explore', 'studyplan', 'accounts', 'profile')
  AND NOT EXISTS (
      SELECT 1 FROM users o
      WHERE o.id <> u.id
        AND lower(o.leetcode_handle) = lower(substring(u.leetcode_handle FROM '(?i)leetcode\.[a-z]+/(?:u/)?([^/?#[:space:]]+)')));
