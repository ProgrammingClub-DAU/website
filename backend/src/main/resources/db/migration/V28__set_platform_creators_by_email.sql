UPDATE users
SET is_platform_creator = FALSE;

UPDATE users
SET is_platform_creator = TRUE
WHERE lower(trim(email)) IN (
    '202401226@dau.ac.in',
    '202401474@dau.ac.in',
    '202401152@dau.ac.in',
    '202401041@dau.ac.in',
    '202401178@dau.ac.in',
    '202403019@dau.ac.in'
);
