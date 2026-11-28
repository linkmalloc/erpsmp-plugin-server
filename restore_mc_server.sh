#!/bin/bash
set -e

SERVER="ubuntu@52.201.251.127"
KEY=".secret/erpsmp-dev.pem"
TAG="$1"

if [ -z "$TAG" ]; then
    echo "Usage: ./restore_mc_server.sh <tag>"
    echo "Available backups on server:"
    ssh -i "$KEY" "$SERVER" 'ls -lh ~/mc-server-backups/*.tar.gz 2>/dev/null || echo "No backups found in ~/mc-server-backups/"'
    exit 1
fi

echo "WARNING: This will replace ~/mc-server with backup tag '$TAG'."
read -p "Type RESTORE to continue: " CONFIRM
if [ "$CONFIRM" != "RESTORE" ]; then
    echo "Aborted."
    exit 1
fi

ssh -i "$KEY" "$SERVER" "TAG='$TAG' bash -s" << 'EOF'
set -e

ARCHIVE=~/mc-server-backups/"${TAG}.tar.gz"
if [ ! -f "$ARCHIVE" ]; then
    echo "[ERROR] Backup not found: $ARCHIVE"
    ls -lh ~/mc-server-backups/*.tar.gz 2>/dev/null || true
    exit 1
fi

echo "[1/4] Stopping Minecraft & Geyser..."
pkill -9 -f "start.sh" >/dev/null 2>&1 || true
screen -wipe >/dev/null 2>&1 || true
screen -ls | grep -E "[0-9]+\.(minecraft|geyser)" | awk '{print $1}' | while read -r s; do
    screen -S "$s" -X quit || true
done
pkill -9 -f "paper.jar" >/dev/null 2>&1 || true
pkill -9 -f "Geyser.jar" >/dev/null 2>&1 || true
sleep 2

echo "[2/4] Moving current ~/mc-server to ~/mc-server-before-restore..."
rm -rf ~/mc-server-before-restore
[ -d ~/mc-server ] && mv ~/mc-server ~/mc-server-before-restore

echo "[3/4] Restoring $ARCHIVE -> ~/mc-server..."
tar -xzf "$ARCHIVE" -C ~

echo "[4/4] Starting restored Minecraft & Geyser..."
cd ~/mc-server
./start_mc_server.sh
EOF

echo "[SUCCESS] Restored ~/mc-server from tag '$TAG'!"
