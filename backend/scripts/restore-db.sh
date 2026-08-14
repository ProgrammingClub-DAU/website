#!/usr/bin/env bash
#
# Restores a dump produced by backup-db.sh.
#
#   ./restore-db.sh backups/cpclub-20260814T000000Z.dump "postgres://u:p@host/db"
#
# DESTRUCTIVE. This replaces the contents of the target database. It refuses to
# run without an explicit target so it cannot silently hit production.
#
# Practise this against a scratch database BEFORE you need it. An untested
# restore procedure is not a recovery plan.

set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <dump-file> <target-connection-uri>" >&2
  echo "" >&2
  echo "The target is required and never defaulted: restoring into the wrong" >&2
  echo "database is exactly the accident this guards against." >&2
  exit 1
fi

DUMP="$1"
TARGET="$2"

[[ -f "$DUMP" ]] || { echo "ERROR: no such dump file: $DUMP" >&2; exit 1; }

# Show what is about to be overwritten, then require a typed confirmation.
echo "About to restore"
echo "  from : $DUMP"
echo "  into : ${TARGET%%\?*}"
echo ""
echo "This OVERWRITES existing data in the target database."
read -r -p "Type RESTORE to continue: " CONFIRM
[[ "$CONFIRM" == "RESTORE" ]] || { echo "Aborted."; exit 1; }

# --clean --if-exists drops existing objects first, so a restore over a partially
# populated database is deterministic rather than a pile of conflicts.
# --no-owner: roles differ between the source and a fresh instance.
# Not using --single-transaction: with --clean, a missing object would abort the
# whole restore. Errors are reported per-object instead.
pg_restore \
  --clean --if-exists \
  --no-owner --no-privileges \
  --dbname="$TARGET" \
  "$DUMP"

echo ""
echo "Restore finished."
echo ""
echo "Now verify before declaring recovery:"
echo "  psql \"\$TARGET\" -c 'SELECT count(*) FROM users;'"
echo "  psql \"\$TARGET\" -c 'SELECT count(*) FROM blog_posts;'"
echo "  psql \"\$TARGET\" -c 'SELECT * FROM flyway_schema_history ORDER BY installed_rank;'"
echo ""
echo "The flyway_schema_history table must be present, or the application will"
echo "try to baseline a database that already has data."
