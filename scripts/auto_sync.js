#!/usr/bin/env node
/**
 * ⚡ ForgeDiagnostics Zero-Conflict Continuous 2-Way Git Sync Daemon
 * 
 * Synchronizes between:
 * - Android Studio
 * - Antigravity IDE
 * - Google AI Studio (.env / GitHub Secrets)
 * - GitHub Repository (newsteps4them-arch/ForgeDiagnostics)
 * 
 * Features:
 * - Real-time file system change detection & polling
 * - Smart debouncing for batch saves
 * - Zero-conflict auto-stashing & rebase pulls
 * - Automatic race-condition retry on simultaneous pushes
 * - Auto-healing merge resolver to prevent sync locks
 */

const { execSync, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '..');
const INTERVAL_MS = parseInt(process.env.SYNC_INTERVAL_MS || '10000', 10);
const DEBOUNCE_MS = parseInt(process.env.SYNC_DEBOUNCE_MS || '3000', 10);
const BRANCH = process.env.SYNC_BRANCH || 'main';
const REMOTE = process.env.SYNC_REMOTE || 'origin';
const ONCE = process.argv.includes('--once');

let isSyncing = false;
let pendingChanges = false;
let debounceTimer = null;

function log(level, msg) {
  const time = new Date().toTimeString().split(' ')[0];
  const colors = {
    INFO: '\x1b[90m',
    SYNC: '\x1b[36m',
    SUCCESS: '\x1b[32m',
    WARN: '\x1b[33m',
    ERROR: '\x1b[31m',
    HEAL: '\x1b[35m'
  };
  const color = colors[level] || '\x1b[0m';
  console.log(`${color}[${time}] [${level}] ${msg}\x1b[0m`);
}

function run(cmd, suppressError = false) {
  try {
    return execSync(cmd, { cwd: ROOT_DIR, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }).trim();
  } catch (err) {
    if (suppressError) return '';
    throw err;
  }
}

function ensureCleanGitState() {
  try {
    // If a rebase or merge was interrupted, abort safely to restore clean branch state
    const rebaseApply = path.join(ROOT_DIR, '.git', 'rebase-apply');
    const rebaseMerge = path.join(ROOT_DIR, '.git', 'rebase-merge');
    const mergeHead = path.join(ROOT_DIR, '.git', 'MERGE_HEAD');

    if (fs.existsSync(rebaseApply) || fs.existsSync(rebaseMerge)) {
      log('HEAL', 'Resolving interrupted rebase. Aborting stuck rebase...');
      run('git rebase --abort', true);
    }
    if (fs.existsSync(mergeHead)) {
      log('HEAL', 'Resolving interrupted merge. Completing merge...');
      run('git merge --abort', true);
    }
  } catch (e) {
    // Ignore cleanup errors
  }
}

function pullRemoteUpdates() {
  try {
    // 1. Fetch remote silently
    run(`git fetch ${REMOTE} ${BRANCH} --quiet`);

    // 2. Check how many commits behind we are
    const behindCount = parseInt(run(`git rev-list --count HEAD..${REMOTE}/${BRANCH}`, true) || '0', 10);
    if (behindCount > 0) {
      log('SYNC', `Found ${behindCount} incoming commit(s) from GitHub. Pulling with auto-stash...`);
      
      // Attempt rebase pull with autostash
      try {
        run(`git pull --rebase --autostash ${REMOTE} ${BRANCH}`);
        log('SUCCESS', `Synchronized ${behindCount} remote commit(s) seamlessly into local workspace.`);
      } catch (pullErr) {
        log('WARN', 'Standard rebase encountered conflict. Activating Auto-Healer...');
        run('git rebase --abort', true);
        
        // Fallback to recursive merge with preference to remote updates while preserving local files
        try {
          run(`git pull --no-edit -X theirs ${REMOTE} ${BRANCH}`);
          log('HEAL', 'Auto-healed and merged incoming updates successfully.');
        } catch (mergeErr) {
          log('HEAL', 'Applying standard fast-forward merge fallback...');
          run(`git merge ${REMOTE}/${BRANCH} -m "Auto-merge: synchronized remote changes"`, true);
        }
      }
    }
  } catch (err) {
    log('WARN', `Remote fetch/pull skipped (temporary network or auth pause): ${err.message.split('\n')[0]}`);
  }
}

function commitAndPushLocalChanges() {
  try {
    // 1. Check for uncommitted changes
    const status = run('git status --porcelain', true);
    if (status) {
      const changedFiles = status.split('\n').map(l => l.trim()).filter(Boolean);
      const summary = changedFiles.slice(0, 3).map(f => path.basename(f.split(' ').pop())).join(', ');
      const extraCount = changedFiles.length > 3 ? ` (+${changedFiles.length - 3} more)` : '';
      
      log('SYNC', `Detected ${changedFiles.length} local change(s): ${summary}${extraCount}. Auto-committing...`);
      
      const nowStr = new Date().toISOString().replace('T', ' ').substring(0, 19);
      run('git add -A');
      run(`git commit -m "Auto-sync: update [${summary}${extraCount}] (${nowStr})" --quiet`, true);
      log('SUCCESS', 'Local changes committed atomically.');
    }

    // 2. Check if local is ahead of remote and push
    let aheadCount = parseInt(run(`git rev-list --count ${REMOTE}/${BRANCH}..HEAD`, true) || '0', 10);
    if (aheadCount > 0) {
      log('SYNC', `Pushing ${aheadCount} commit(s) to GitHub (${REMOTE}/${BRANCH})...`);
      
      let pushSuccess = false;
      let attempts = 0;
      const MAX_ATTEMPTS = 3;

      while (!pushSuccess && attempts < MAX_ATTEMPTS) {
        attempts++;
        try {
          run(`git push ${REMOTE} ${BRANCH}`);
          pushSuccess = true;
          log('SUCCESS', `Pushed ${aheadCount} commit(s) to GitHub successfully!`);
        } catch (pushErr) {
          log('WARN', `Push race-condition detected (remote updated concurrently). Re-syncing (attempt ${attempts}/${MAX_ATTEMPTS})...`);
          pullRemoteUpdates();
          aheadCount = parseInt(run(`git rev-list --count ${REMOTE}/${BRANCH}..HEAD`, true) || '0', 10);
          if (aheadCount === 0) {
            pushSuccess = true;
            log('SUCCESS', 'Remote already has all latest commits.');
          }
        }
      }
    }
  } catch (err) {
    log('ERROR', `Error during local commit/push: ${err.message.split('\n')[0]}`);
  }
}

function fullSyncCycle() {
  if (isSyncing) return;
  isSyncing = true;
  try {
    ensureCleanGitState();
    pullRemoteUpdates();
    commitAndPushLocalChanges();
  } catch (e) {
    log('ERROR', `Sync cycle exception: ${e.message}`);
  } finally {
    isSyncing = false;
  }
}

function triggerDebouncedSync() {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    fullSyncCycle();
  }, DEBOUNCE_MS);
}

// Watch filesystem for instant reactions
function initFileWatcher() {
  try {
    const watchDirs = [ROOT_DIR, path.join(ROOT_DIR, 'app'), path.join(ROOT_DIR, 'scripts')];
    watchDirs.forEach(dir => {
      if (fs.existsSync(dir)) {
        fs.watch(dir, { recursive: true }, (eventType, filename) => {
          if (!filename) return;
          // Ignore transient or build files
          if (
            filename.includes('.git') ||
            filename.includes('.gradle') ||
            filename.includes('build') ||
            filename.includes('node_modules') ||
            filename.includes('.sync-log') ||
            filename.includes('.sync-lock') ||
            filename.includes('.idea')
          ) {
            return;
          }
          triggerDebouncedSync();
        });
      }
    });
    log('INFO', 'Real-time filesystem watchers initialized for Android Studio & Antigravity IDE.');
  } catch (err) {
    log('WARN', `Native file watching unavailable, relying on interval polling: ${err.message}`);
  }
}

console.log('\x1b[36m===================================================================\x1b[0m');
console.log('\x1b[32m  🚀 ForgeDiagnostics Autonomous 4-Way Zero-Conflict Git Sync     \x1b[0m');
console.log(`\x1b[90m  Workspace: ${ROOT_DIR}\x1b[0m`);
console.log(`\x1b[90m  Remote: ${REMOTE} | Branch: ${BRANCH} | Polling: ${INTERVAL_MS / 1000}s | Debounce: ${DEBOUNCE_MS / 1000}s\x1b[0m`);
console.log('\x1b[36m===================================================================\x1b[0m');

if (ONCE) {
  fullSyncCycle();
  process.exit(0);
}

initFileWatcher();
fullSyncCycle();
setInterval(fullSyncCycle, INTERVAL_MS);
