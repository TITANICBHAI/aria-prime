#!/usr/bin/env bash
set -euo pipefail

COMMIT_MESSAGE="${COMMIT_MESSAGE:-chore: automated push}"

git config user.email "agent@aria-prime.local" 2>/dev/null || true
git config user.name  "ARIA Agent"             2>/dev/null || true

git add -A

if git --no-optional-locks diff --cached --quiet; then
  echo "Nothing to commit — working tree clean."
  exit 0
fi

git commit -m "$COMMIT_MESSAGE"

GITHUB_TOKEN="${GITHUB_PERSONAL_ACCESS_TOKEN:-${GITHUB_TOKEN:-}}"
REMOTE_URL="$(git remote get-url origin)"

if [ -n "$GITHUB_TOKEN" ]; then
  AUTHED_URL="$(echo "$REMOTE_URL" | sed "s|https://|https://x-access-token:${GITHUB_TOKEN}@|")"
  git push "$AUTHED_URL" HEAD:main
else
  git push origin main
fi

echo "Push complete."
