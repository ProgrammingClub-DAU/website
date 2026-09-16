-- Sign-in moved to Google, so the application no longer sets a password.
--
-- Members authenticate with their @dau.ac.in Google account and the server
-- issues its own JWT from the verified ID token. Nothing reads or writes
-- users.password any more, so accounts created from here on have none.
--
-- The column is relaxed rather than dropped. Dropping it would destroy the
-- existing hashes irreversibly, and there is no reason to: the hashes are
-- inert once no code path compares against them, and keeping the column means
-- this migration can be undone by restoring the NOT NULL if the club ever
-- decides to offer passwords again.

ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
