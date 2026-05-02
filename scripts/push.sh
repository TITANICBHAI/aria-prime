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

GITHUB_TOKEN="${GITHUB_TOKEN:-}"
if [ -n "$GITHUB_TOKEN" ]; then
  git -c "http.extraheader=Authorization: Bearer ${GITHUB_TOKEN}" push origin main
else
  git push origin main
fi

echo "Push complete."
