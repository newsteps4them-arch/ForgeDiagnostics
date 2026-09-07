#!/usr/bin/env bash
set -e

EXT_DIR=".gemini/extensions/stitch"
REPO_URL="https://github.com/gemini-cli-extensions/stitch"

echo "[+] Setting up local Stitch Gemini CLI extension..."

mkdir -p .gemini/extensions

if [ -d "$EXT_DIR/.git" ]; then
    echo "[+] Extension already cloned at $EXT_DIR. Pulling latest changes..."
    git -C "$EXT_DIR" pull
else
    echo "[+] Cloning $REPO_URL into $EXT_DIR..."
    git clone "$REPO_URL" "$EXT_DIR"
fi

echo "[+] Local Stitch extension ready at $EXT_DIR."
