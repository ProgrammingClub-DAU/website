# Database Backup & Recovery

Every registered member account, linked Codeforces handle and blog post lives in
one PostgreSQL database. This document is how it is protected and how it is
restored.

> **This replaces the previous procedure** in the Phase 1 playbook, which read
> *"If migrations corrupt data, drop schema and restore from local backup SQL
> dump."* That was unsafe on three counts: no backup existed, no restore steps
> were written, and dropping the schema under `ddl-auto: validate` leaves a
> service that cannot start at all. Dropping the schema is the incident, not the
> recovery.

---

## Take a backup

From `backend/`:

```bash
./scripts/backup-db.sh "postgres://user:password@host:5432/dbname"
```

Or, with the environment already configured (`DATABASE_URL`, or the
`SPRING_DATASOURCE_*` set):

```bash
./scripts/backup-db.sh
```

Writes a compressed, timestamped archive to `./backups/` and verifies it is
readable before reporting success — a dump that cannot be listed is not a backup.

**Do this now, before anything else in this document is needed.** If the club is
on a free Postgres tier, note that those commonly have no point-in-time recovery
and expire the database after a fixed period. The dump you take today may be the
only copy that exists.

`./backups/` is gitignored. Keep copies somewhere that is not one laptop.

## Restore a backup

```bash
./scripts/restore-db.sh backups/cpclub-20260814T000000Z.dump "postgres://user:pass@host:5432/dbname"
```

Destructive, requires an explicit target, and asks for typed confirmation. It
prints verification queries afterwards — run them. A restore you have not
verified is a hope.

## Practise it

**Restore into a scratch database before you need this.** An untested restore is
not a recovery plan, and the moment you need it is the worst moment to discover
`pg_dump` version mismatches or a missing client tool.

```bash
docker compose up -d                      # local Postgres from the repo root
createdb -h localhost -U cpclub_user restore_test
./scripts/restore-db.sh backups/<file>.dump \
    "postgres://cpclub_user:cpclub_password@localhost:5432/restore_test"
```

If that works, the procedure works.

---

## How this interacts with migrations

Schema changes are versioned in `backend/src/main/resources/db/migration/` and
applied by Flyway. Production runs `ddl-auto: validate`, so Hibernate never
alters anything — the migrations are the only thing that changes the schema.

Two consequences worth understanding:

**A dump includes `flyway_schema_history`.** Restoring brings the migration
history with it, so the application knows which migrations have run. If that
table is missing after a restore, the app will try to baseline a database that
already has data. The restore script's verification step checks for it.

**Rolling code back does not roll the database back.** If a deploy applied a
migration and you revert the code, the schema still has the change, and
`validate` will fail against the older entities. Recovering from a bad migration
means either writing a forward migration that undoes it, or restoring a dump
taken before it ran.

**So: take a backup before deploying a migration.** That is the single habit that
makes the rest of this recoverable.

---

## Incident checklist

1. **Stop writes** if data is actively being corrupted — scale the backend to
   zero in the hosting dashboard.
2. **Take a dump of the current broken state anyway.** It costs a minute and it
   is the only way back if the restore turns out worse.
3. Restore the most recent good dump into a **scratch** database first, and
   verify the row counts look right.
4. Only then restore into production.
5. Bring the backend back up and confirm `/api/health` responds and a known
   member appears on `/members`.

Never drop the schema as a recovery step. Under `validate` with no schema, the
application does not start, and the failure becomes total instead of partial.
