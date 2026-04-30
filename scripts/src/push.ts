// Push aria-prime to GitHub.
//
// This repo is checked out as a subdirectory of a larger pnpm monorepo whose
// own .git tracks every workspace file. The GitHub repo at
// https://github.com/TITANICBHAI/aria-prime is supposed to contain only the
// Android app — the contents of aria-prime/. So we:
//   1. Clone the GitHub repo into a temp dir.
//   2. Mirror the contents of aria-prime/ into that clone (excluding build
//      outputs, IDE noise, model weights, keystores, etc.).
//   3. Commit and push using the user's PAT injected via http.extraheader so
//      the token is never written to .git/config.
//
// Required env (any one of these is accepted, checked in order):
//   GITHUB_TOKEN
//   GITHUB_PERSONAL_ACCESS_TOKEN
//
// Optional env:
//   REMOTE_URL       default: https://github.com/TITANICBHAI/aria-prime.git
//   REMOTE_BRANCH    default: main
//   COMMIT_MESSAGE   default: a generated message with timestamp
//
// Usage (from the workspace root):
//   tsx aria-prime/scripts/src/push.ts
// Or via the Push to GitHub workflow.

import { execFileSync, spawnSync } from "node:child_process";
import {
  cpSync,
  existsSync,
  mkdtempSync,
  readdirSync,
  rmSync,
  statSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));

// Resolve the aria-prime directory. The same script file lives in two places:
//   /home/runner/workspace/scripts/src/push.ts       (the @workspace/scripts package)
//   /home/runner/workspace/aria-prime/scripts/src/push.ts (mirrored into the app dir)
// Try, in order: ARIA_DIR env, ../.. from this script, ../../aria-prime from
// this script, then fail with a clear message. Each candidate is verified by
// the presence of a known marker file so we never walk the wrong tree.
function findAriaDir(): string {
  const ariaMarker = "AGENT_INSTRUCTIONS.md";
  const candidates = [
    process.env.ARIA_DIR,
    resolve(SCRIPT_DIR, "..", ".."),
    resolve(SCRIPT_DIR, "..", "..", "aria-prime"),
  ].filter((p): p is string => Boolean(p));
  for (const c of candidates) {
    if (existsSync(join(c, ariaMarker))) return c;
  }
  console.error(`[push] ERROR: could not locate aria-prime directory.`);
  console.error(`[push] tried (in order):`);
  for (const c of candidates) console.error(`[push]   - ${c}`);
  console.error(
    `[push] set ARIA_DIR=/path/to/aria-prime to override.`,
  );
  process.exit(1);
}

const ARIA_DIR = findAriaDir();
const WORKSPACE_ROOT = resolve(ARIA_DIR, "..");

const REMOTE_URL =
  process.env.REMOTE_URL || "https://github.com/TITANICBHAI/aria-prime.git";
const REMOTE_BRANCH = process.env.REMOTE_BRANCH || "main";

// Paths inside aria-prime/ that must not be pushed. Mirrors aria-prime/.gitignore.
const EXCLUDE_DIRS = new Set([
  ".git",
  "build",
  ".gradle",
  ".idea",
  ".cxx",
  ".vscode",
  ".idx",
  "build-logs",
  "node_modules",
  "llama.cpp", // vendored at build time only
  "captures",
]);
const EXCLUDE_FILES_EXACT = new Set([
  "local.properties",
  ".DS_Store",
  "Thumbs.db",
]);
const EXCLUDE_EXT = new Set([".iml", ".gguf", ".tflite", ".jks", ".keystore"]);

function log(msg: string): void {
  console.log(`[push] ${msg}`);
}

function fail(msg: string): never {
  console.error(`[push] ERROR: ${msg}`);
  process.exit(1);
}

function resolveToken(): string {
  const t = process.env.GITHUB_TOKEN || process.env.GITHUB_PERSONAL_ACCESS_TOKEN;
  if (!t) {
    fail(
      "neither GITHUB_TOKEN nor GITHUB_PERSONAL_ACCESS_TOKEN is set in the environment.",
    );
  }
  return t;
}

function authHeaderValue(token: string): string {
  // Same format the previous bash push script used. x-access-token is the
  // username GitHub expects with a PAT for git over HTTPS.
  const b64 = Buffer.from(`x-access-token:${token}`, "utf8").toString("base64");
  return `Authorization: Basic ${b64}`;
}

function runGit(args: string[], opts: { cwd: string; authHeader?: string }): void {
  const fullArgs = opts.authHeader
    ? ["-c", `http.extraheader=${opts.authHeader}`, ...args]
    : args;
  const printable = args.join(" ");
  log(`git ${printable}`);
  const r = spawnSync("git", fullArgs, {
    cwd: opts.cwd,
    stdio: ["ignore", "inherit", "inherit"],
  });
  if (r.status !== 0) {
    fail(`git ${printable} exited with code ${r.status}`);
  }
}

function gitOutput(args: string[], cwd: string): string {
  return execFileSync("git", args, { cwd, encoding: "utf8" }).trim();
}

function shouldSkip(srcRelPath: string, isDir: boolean): boolean {
  const segments = srcRelPath.split("/").filter((s) => s.length > 0);
  for (const seg of segments) {
    if (EXCLUDE_DIRS.has(seg)) return true;
  }
  if (!isDir) {
    const last = segments[segments.length - 1] ?? "";
    if (EXCLUDE_FILES_EXACT.has(last)) return true;
    const dot = last.lastIndexOf(".");
    if (dot >= 0) {
      const ext = last.slice(dot);
      if (EXCLUDE_EXT.has(ext)) return true;
    }
  }
  return false;
}

function wipeDestExceptGit(dest: string): void {
  for (const entry of readdirSync(dest)) {
    if (entry === ".git") continue;
    rmSync(join(dest, entry), { recursive: true, force: true });
  }
}

function mirrorTree(src: string, dest: string): { copied: number; skipped: number } {
  let copied = 0;
  let skipped = 0;

  function walk(currentSrc: string): void {
    const entries = readdirSync(currentSrc, { withFileTypes: true });
    for (const e of entries) {
      const absSrc = join(currentSrc, e.name);
      const rel = relative(src, absSrc);

      // For symlinks, stat the target so we treat a symlinked dir as a dir
      // (and skip it via shouldSkip in the same way).
      let isDir: boolean;
      let isFile: boolean;
      if (e.isSymbolicLink()) {
        try {
          const st = statSync(absSrc);
          isDir = st.isDirectory();
          isFile = st.isFile();
        } catch {
          // Dangling symlink — skip silently.
          skipped += 1;
          continue;
        }
      } else {
        isDir = e.isDirectory();
        isFile = e.isFile();
      }

      if (shouldSkip(rel, isDir)) continue;

      const absDest = join(dest, rel);
      if (isDir) {
        walk(absSrc);
      } else if (isFile) {
        cpSync(absSrc, absDest, { recursive: false, dereference: true });
        copied += 1;
      } else {
        // Block devices, sockets, etc. — never expected in a source tree.
        skipped += 1;
      }
    }
  }

  walk(src);
  return { copied, skipped };
}

function main(): void {
  log(`workspace root: ${WORKSPACE_ROOT}`);
  log(`aria-prime dir: ${ARIA_DIR}`);
  log(`remote        : ${REMOTE_URL}`);
  log(`branch        : ${REMOTE_BRANCH}`);

  if (!existsSync(ARIA_DIR) || !statSync(ARIA_DIR).isDirectory()) {
    fail(`aria-prime directory not found at ${ARIA_DIR}`);
  }

  const token = resolveToken();
  const authHeader = authHeaderValue(token);

  const tmp = mkdtempSync(join(tmpdir(), "aria-push-"));
  const repoDir = join(tmp, "repo");
  log(`temp dir       : ${tmp}`);

  try {
    // 1. Shallow clone the GitHub repo (no token needed for public repos, but
    // pass the auth header anyway so private/forked repos also work).
    runGit(
      [
        "clone",
        "--depth=1",
        `--branch=${REMOTE_BRANCH}`,
        REMOTE_URL,
        repoDir,
      ],
      { cwd: tmp, authHeader },
    );

    // 2. Set committer identity inside the clone so the commit succeeds.
    runGit(["config", "user.email", "replit-agent@users.noreply.github.com"], {
      cwd: repoDir,
    });
    runGit(["config", "user.name", "Replit Agent"], { cwd: repoDir });

    // 3. Mirror aria-prime/ into the clone (preserving its .git).
    log("wiping working tree (preserving .git)...");
    wipeDestExceptGit(repoDir);
    log("mirroring aria-prime/ into the clone...");
    const { copied, skipped } = mirrorTree(ARIA_DIR, repoDir);
    log(`mirrored ${copied} files (skipped ${skipped} non-regular entries)`);

    // 4. Stage everything.
    runGit(["add", "-A"], { cwd: repoDir });

    // 5. Bail early if nothing changed (clean exit).
    const diffCheck = spawnSync("git", ["diff", "--cached", "--quiet"], {
      cwd: repoDir,
    });
    if (diffCheck.status === 0) {
      log("no changes vs remote — nothing to push.");
      return;
    }

    // 6. Commit.
    const stamp = new Date().toISOString().replace(/[.]\d{3}Z$/, "Z");
    const message =
      process.env.COMMIT_MESSAGE ||
      `chore: sync aria-prime from Replit (${stamp})`;
    runGit(["commit", "-m", message], { cwd: repoDir });
    const sha = gitOutput(["rev-parse", "HEAD"], repoDir);
    log(`committed ${sha}`);

    // 7. Push.
    runGit(["push", "origin", `HEAD:${REMOTE_BRANCH}`], {
      cwd: repoDir,
      authHeader,
    });

    // 8. Report.
    const slug = REMOTE_URL.replace(/^https:\/\/github\.com\//, "")
      .replace(/\.git$/, "");
    log(`OK: pushed ${sha} -> ${REMOTE_URL} ${REMOTE_BRANCH}`);
    log(`view: https://github.com/${slug}/commit/${sha}`);
  } finally {
    rmSync(tmp, { recursive: true, force: true });
  }
}

main();
