#!/usr/bin/env bash
#
# Runs FirstSyncBenchmarkTest on one or more commits and prints the results side
# by side, so a change to the first Codeforces sync can be measured against the
# code before it.
#
#   backend/scripts/run-first-sync-benchmark.sh               # 1ab364c^ (before batching) and HEAD
#   backend/scripts/run-first-sync-benchmark.sh <commit>...   # any commits
#
#   SOLVES=1500 RUNS=5 backend/scripts/run-first-sync-benchmark.sh   # the defaults
#
# Each commit is checked out into a temporary git worktree, the current benchmark
# file is copied in (older commits do not have it), and the test runs against a
# PostgreSQL container. Needs Docker running. The full build log for each commit
# is kept in backend/target/benchmark-<sha>.log.
#
# Expect a few minutes per commit: the code before batching takes about 20 s per
# run, and each scenario does one warm-up and RUNS timed runs.

set -euo pipefail

SOLVES="${SOLVES:-1500}"
RUNS="${RUNS:-5}"

ROOT="$(git rev-parse --show-toplevel)"
BENCH_REL="backend/src/test/java/com/cpclub/backend/codeforces/FirstSyncBenchmarkTest.java"
LOG_DIR="$ROOT/backend/target"

if [[ $# -gt 0 ]]; then
  COMMITS=("$@")
else
  COMMITS=("1ab364c^" "HEAD")
fi

docker info >/dev/null 2>&1 || { echo "Docker is not running; the benchmark needs it for PostgreSQL." >&2; exit 1; }

WORK="$(mktemp -d)"
WORKTREES=()
cleanup() {
  for dir in "${WORKTREES[@]}"; do
    git -C "$ROOT" worktree remove --force "$dir" >/dev/null 2>&1 || true
  done
  rm -rf "$WORK"
}
trap cleanup EXIT

mkdir -p "$LOG_DIR"

echo "First-sync benchmark: ${SOLVES} solves, ${RUNS} timed runs per scenario"
echo "Machine: $(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2 | xargs || uname -m)," \
     "$(nproc 2>/dev/null || echo '?') logical CPUs, $(uname -s)"
echo "Docker:  $(docker version --format '{{.Server.Version}}' 2>/dev/null || echo unknown)"
echo

for commit in "${COMMITS[@]}"; do
  sha="$(git -C "$ROOT" rev-parse --short "$commit")"
  dir="$WORK/$sha"
  log="$LOG_DIR/benchmark-$sha.log"

  git -C "$ROOT" worktree add --detach "$dir" "$sha" >/dev/null 2>&1
  WORKTREES+=("$dir")
  cp "$ROOT/$BENCH_REL" "$dir/$BENCH_REL"
  chmod +x "$dir/backend/mvnw"

  echo "== $commit ($sha) -- running, log in backend/target/benchmark-$sha.log"
  if (cd "$dir/backend" && ./mvnw -B test -Dtest=FirstSyncBenchmarkTest -Dbenchmark=true \
        -Dbenchmark.solves="$SOLVES" -Dbenchmark.runs="$RUNS") >"$log" 2>&1; then
    grep '^BENCH' "$log" | sed "s/^BENCH/  /"
  else
    echo "  failed; see $log" >&2
  fi
  echo
done

echo "Equal 'rows' fingerprints mean both versions stored identical solves."
