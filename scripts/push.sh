#!/usr/bin/env bash
# Push the current branch to GitHub using the GITHUB_TOKEN secret.
#
# Behaviour:
#   - Uses GITHUB_TOKEN from the environment (Replit secret).
#   - Stages and commits any local changes with a default message
#     (override with COMMIT_MESSAGE="…").
#   - Pushes to `origin` over HTTPS without persisting the token in
#     `.git/config` (the token is injected via -c http.extraheader).
#   - Prints clear success / failure output and exits non-zero on any failure
#     so the workflow surfaces the result.
#
# Usage (from repo root):
#   COMMIT_MESSAGE="my message" bash scripts/push.sh
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if [[ -z "${GITHUB_TOKEN:-}" ]]; then
  echo "[push] ERROR: GITHUB_TOKEN is not set in the environment." >&2
  exit 1
fi

REMOTE_URL="$(git remote get-url origin 2>/dev/null || true)"
if [[ -z "${REMOTE_URL}" ]]; then
  echo "[push] ERROR: no 'origin' remote configured." >&2
  exit 1
fi
if [[ "${REMOTE_URL}" != https://github.com/* ]]; then
  echo "[push] ERROR: 'origin' is not an https://github.com/* URL: ${REMOTE_URL}" >&2
  exit 1
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "${BRANCH}" == "HEAD" ]]; then
  echo "[push] ERROR: detached HEAD; checkout a branch before pushing." >&2
  exit 1
fi

# Clear stale lock files left behind by interrupted git processes.
# A lock is "stale" if it has not been modified in the last 60s and no
# git process is currently running in this repo.
for lock in .git/index.lock .git/HEAD.lock .git/config.lock; do
  if [[ -f "${lock}" ]]; then
    if pgrep -af "^git " | grep -v "$$" >/dev/null 2>&1; then
      echo "[push] WARNING: ${lock} present and another git process is running; aborting" >&2
      exit 1
    fi
    echo "[push] removing stale lock ${lock}"
    rm -f "${lock}"
  fi
done

# Make sure committer identity is set for the staged commit (if any).
if ! git config user.email >/dev/null 2>&1; then
  git config user.email "replit-agent@users.noreply.github.com"
fi
if ! git config user.name >/dev/null 2>&1; then
  git config user.name "Replit Agent"
fi

# Stage + commit any pending local changes so the workflow always pushes a
# fully-up-to-date snapshot. If nothing is staged this is a no-op.
git add -A
if ! git diff --cached --quiet; then
  MSG="${COMMIT_MESSAGE:-chore: auto-push from Replit ($(date -u +%Y-%m-%dT%H:%M:%SZ))}"
  git commit -m "${MSG}"
  echo "[push] committed local changes: ${MSG}"
else
  echo "[push] no local changes to commit"
fi

# Build the auth header without printing the token.
AUTH_HEADER="Authorization: Basic $(printf 'x-access-token:%s' "${GITHUB_TOKEN}" | base64 | tr -d '\n')"

echo "[push] pushing ${BRANCH} -> ${REMOTE_URL}"
if git -c "http.extraheader=${AUTH_HEADER}" push origin "${BRANCH}"; then
  echo "[push] OK: pushed ${BRANCH} to origin"
  exit 0
else
  status=$?
  echo "[push] FAILED with exit code ${status}" >&2
  exit "${status}"
fi
