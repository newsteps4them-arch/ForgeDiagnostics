#!/usr/bin/env node
/**
 * Cross-platform 2-Way Git Sync Daemon for ForgeDiagnostics
 * Runs in Node.js, compatible with Windows, macOS, and Linux.
 */

const { execSync } = require('child_process');

const INTERVAL_MS = parseInt(process.env.SYNC_INTERVAL_MS || '15000', 10);
const BRANCH = process.env.SYNC_BRANCH || 'main';
const REMOTE = process.env.SYNC_REMOTE || 'origin';
const ONCE = process.argv.includes('--once');

function run(cmd, suppressError = false) {
  try {
    return execSync(cmd, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }).trim();
  } catch (err) {
    if (suppressError) return '';
    throw err;
  }
}

function log(level, msg) {
  const time = new Date().toTimeString().split(' ')[0];
  const prefix = {
    INFO: '\x1b[90m',
    SYNC: '\x1b[36m',
    SUCCESS: '\x1b[32m',
    WARN: '\x1b[33m',
    ERROR: '\x1b[31m',
  }[level] || '\x1b[0m';
  console.log(`${prefix}[${time}] [${level}] ${msg}\x1b[0m`);
}

function syncCycle() {
  try {
    // 1. Fetch remote updates
    try {
      run(`git fetch ${REMOTE} --quiet`);
    } catch {
      log('WARN', `Remote '${REMOTE}' unreachable or offline. Retrying next cycle...`);
      return;
    }

    // 2. Check if behind remote and pull
    const behindCount = parseInt(run(`git rev-list --count HEAD..${REMOTE}/${BRANCH}`, true) || '0', 10);
    if (behindCount > 0) {
      log('SYNC', `GitHub has ${behindCount} new commit(s). Pulling into workspace...`);
      try {
        run(`git pull --rebase --autostash ${REMOTE} ${BRANCH}`);
        log('SUCCESS', 'Successfully pulled remote updates with autostash.');
      } catch {
        log('WARN', 'Rebase pull failed, attempting standard merge pull...');
        run('git rebase --abort', true);
        run(`git pull --no-edit ${REMOTE} ${BRANCH}`, true);
      }
    }

    // 3. Check for local modifications/untracked files
    const status = run('git status --porcelain', true);
    if (status) {
      log('SYNC', 'Local file changes detected. Auto-committing...');
      const dateStr = new Date().toISOString().replace('T', ' ').substring(0, 19);
      run('git add -A');
      run(`git commit -m "Auto-sync: workspace update [${dateStr}]" --quiet`, true);
      log('SUCCESS', 'Local changes committed.');
    }

    // 4. Check if local is ahead and push
    const aheadCount = parseInt(run(`git rev-list --count ${REMOTE}/${BRANCH}..HEAD`, true) || '0', 10);
    if (aheadCount > 0) {
      log('SYNC', `Pushing ${aheadCount} commit(s) to GitHub (${REMOTE}/${BRANCH})...`);
      try {
        run(`git push ${REMOTE} ${BRANCH}`);
        log('SUCCESS', 'Changes pushed to GitHub successfully!');
      } catch (pushErr) {
        log('ERROR', `Push failed: ${pushErr.message.split('\n')[0]}`);
      }
    }
  } catch (cycleErr) {
    log('ERROR', `Sync error: ${cycleErr.message}`);
  }
}

console.log('\x1b[36m=========================================================\x1b[0m');
console.log('\x1b[32m   Team Forge 2-Way Git Continuous Sync Daemon Active    \x1b[0m');
console.log(`\x1b[90m   Branch: ${BRANCH} | Remote: ${REMOTE} | Interval: ${INTERVAL_MS / 1000}s\x1b[0m`);
console.log('\x1b[36m=========================================================\x1b[0m');

if (ONCE) {
  syncCycle();
  process.exit(0);
}

log('INFO', 'Continuous sync loop running in background. Monitoring changes...');
syncCycle();
setInterval(syncCycle, INTERVAL_MS);
