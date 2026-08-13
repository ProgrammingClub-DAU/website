#!/usr/bin/env bash
#
# Takes a compressed, timestamped logical backup of the production database.
#
#   ./backup-db.sh                       # uses $SPRING_DATASOURCE_* / $DATABASE_URL
#   ./backup-db.sh "postgres://u:p@h/db" # or pass a connection URI directly
#
# Restore with restore-db.sh. Both are documented in documents/DATABASE-BACKUP.md.
#
# Requires the postgresql client tools (pg_dump). Verify the version matches or
# exceeds the server: an older pg_dump refuses to dump a newer server.

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="${BACKUP_DIR}/cpclub-${STAMP}.dump"

# Accept either a URI argument, the platform-injected DATABASE_URL, or the
# individual Spring variables. Render injects DATABASE_URL; Spring reads the
# SPRING_DATASOURCE_* set. Both are handled so this works wherever it is run.
if [[ $# -ge 1 ]]; then
  CONN="$1"
elif [[ -n "${DATABASE_URL:-}" ]]; then
  CONN="$DATABASE_URL"
elif [[ -n "${SPRING_DATASOURCE_URL:-}" ]]; then
  # jdbc:postgresql://host:5432/db -> postgres://user:pass@host:5432/db
  HOSTPART="${SPRING_DATASOURCE_URL#jdbc:postgresql://}"
  CONN="postgres://${SPRING_DATASOURCE_USERNAME}:${SPRING_DATASOURCE_PASSWORD}@${HOSTPART}"
else
  echo "ERROR: no connection details." >&2
  echo "Pass a URI, or set DATABASE_URL, or set SPRING_DATASOURCE_URL/USERNAME/PASSWORD." >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"

echo "Backing up to ${OUT} ..."
# -Fc: custom format, compressed and restorable selectively by pg_restore.
# --no-owner / --no-privileges: restorable into a database with different roles,
# which is what you need when restoring into a fresh instance after an incident.
pg_dump --format=custom --no-owner --no-privileges --file="$OUT" "$CONN"

SIZE="$(du -h "$OUT" | cut -f1)"
echo "Done: ${OUT} (${SIZE})"

# A backup nobody has restored is a guess, not a backup. This is the cheapest
# possible integrity check — it proves the file parses and lists real objects.
TABLES="$(pg_restore --list "$OUT" | grep -c 'TABLE DATA' || true)"
echo "Verified: archive is readable and contains ${TABLES} table(s) of data."

if [[ "$TABLES" -eq 0 ]]; then
  echo "WARNING: no table data in this dump. Check you pointed at the right database." >&2
  exit 1
fi
