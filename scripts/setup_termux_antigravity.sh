#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# 🚀 ForgeDiagnostics: Termux Antigravity CLI Mobile Setup
# Enables full development access to ForgeDiagnostics & Antigravity CLI on Android
# ==============================================================================

set -e

echo "📱 Setting up ForgeDiagnostics Antigravity CLI in Termux..."

# 1. Update Termux base packages
pkg update -y && pkg upgrade -y

# 2. Install essential development packages
pkg install -y git nodejs openssh gh curl jq python

# 3. Setup workspace directory
WORKDIR="$HOME/ForgeDiagnostics"
if [ ! -d "$WORKDIR" ]; then
    echo "📦 Cloning ForgeDiagnostics repository..."
    git clone https://github.com/newsteps4them-arch/ForgeDiagnostics.git "$WORKDIR"
else
    echo "🔄 Updating existing ForgeDiagnostics repository..."
    cd "$WORKDIR" && git pull --rebase origin main
fi

cd "$WORKDIR"

# 4. Install project dependencies
npm install

# 5. Configure Antigravity CLI alias and helper commands in .bashrc
BASHRC="$HOME/.bashrc"
touch "$BASHRC"

if ! grep -q "FORGE_ANTIGRAVITY_SETUP" "$BASHRC"; then
    cat << 'EOF' >> "$BASHRC"

# >>> FORGE_ANTIGRAVITY_SETUP >>>
export FORGE_ROOT="$HOME/ForgeDiagnostics"
alias forge="cd $FORGE_ROOT"
alias forge:sync="cd $FORGE_ROOT && node scripts/auto_sync.js --once"
alias forge:watch="cd $FORGE_ROOT && node scripts/auto_sync.js"
alias forge:test="cd $FORGE_ROOT && npm test"
alias forge:status="cd $FORGE_ROOT && npm run sync:status"

# Antigravity CLI remote / local bridge
agy() {
    if command -v agy &> /dev/null; then
        command agy "$@"
    else
        echo "⚡ Running ForgeDiagnostics Agent Runner..."
        cd "$FORGE_ROOT" && node scripts/auto_sync.js --once
    fi
}
# <<< FORGE_ANTIGRAVITY_SETUP <<<
EOF
fi

echo ""
echo "=================================================================="
echo "✅ ForgeDiagnostics Termux Setup Complete!"
echo "   - Project Folder: ~/ForgeDiagnostics"
echo "   - Run 'forge' to jump to your project"
echo "   - Run 'forge:watch' to start 2-way real-time auto-sync"
echo "   - Run 'forge:test' to execute test suite on Android"
echo "=================================================================="
