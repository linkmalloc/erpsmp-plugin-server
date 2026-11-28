#!/bin/bash
set -e

SERVER="ubuntu@52.201.251.127"
KEY=".secret/erpsmp-dev.pem"
TAG="${1:-backup-$(date +%Y%m%d-%H%M%S)}"

# Sanitize tag name
TAG=$(echo "$TAG" | tr -cd 'a-zA-Z0-9._-')
if [ -z "$TAG" ]; then
    echo "[ERROR] Invalid tag name."
    exit 1
fi

echo "[INFO] Creating tagged backup of ~/mc-server as '$TAG'..."

ssh -i "$KEY" "$SERVER" "TAG='$TAG' bash -s" << 'EOF'
set -e

mkdir -p ~/mc-server-backups
OUT=~/mc-server-backups/"${TAG}.tar.gz"
if [ -f "$OUT" ]; then
    echo "[ERROR] Backup tag '$TAG' already exists at $OUT. Choose a different tag."
    exit 1
fi

echo "[INFO] Disk space before backup:"
df -h /

cd ~/mc-server
echo "[1/3] Stopping Minecraft & Geyser cleanly for consistent snapshot..."
pkill -9 -f "start.sh" >/dev/null 2>&1 || true
screen -wipe >/dev/null 2>&1 || true
screen -ls | grep -E "[0-9]+\.(minecraft|geyser)" | awk '{print $1}' | while read -r s; do
    screen -S "$s" -X quit || true
done
pkill -9 -f "paper.jar" >/dev/null 2>&1 || true
pkill -9 -f "Geyser.jar" >/dev/null 2>&1 || true
sleep 2

echo "[2/3] Archiving ~/mc-server -> $OUT..."
tar -czf "$OUT" -C ~ mc-server
ls -lh "$OUT"

echo "[3/3] Restarting Minecraft & Geyser..."
cd ~/mc-server
./start_mc_server.sh
EOF

echo "[SUCCESS] Backup tagged '$TAG' saved on server at ~/mc-server-backups/${TAG}.tar.gz"
echo "To restore later: ./restore_mc_server.sh $TAG"
