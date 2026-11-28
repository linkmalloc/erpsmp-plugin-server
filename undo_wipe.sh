#!/bin/bash
set -e

SERVER="ubuntu@52.201.251.127"
KEY=".secret/erpsmp-dev.pem"

ssh -i "$KEY" "$SERVER" 'bash -s' << 'EOF'
set -e

LATEST_BACKUP=$(ls -dt ~/wipe-backup-* 2>/dev/null | head -n 1 || true)
if [ -z "$LATEST_BACKUP" ]; then
    echo "[ERROR] No ~/wipe-backup-* directory found on server."
    exit 1
fi
echo "[INFO] Restoring from $LATEST_BACKUP..."

cd ~/mc-server
pkill -9 -f "start.sh" >/dev/null 2>&1 || true
screen -wipe >/dev/null 2>&1 || true
screen -ls | grep -E "[0-9]+\.(minecraft|geyser)" | awk '{print $1}' | while read -r s; do
    screen -S "$s" -X quit || true
done
pkill -9 -f "paper.jar" >/dev/null 2>&1 || true
pkill -9 -f "Geyser.jar" >/dev/null 2>&1 || true
sleep 2

rm -f plugins/ScoreboardPlugin/playerdata.db*
[ -f "$LATEST_BACKUP/playerdata.db" ] && cp "$LATEST_BACKUP/playerdata.db"* plugins/ScoreboardPlugin/
[ -f "$LATEST_BACKUP/config.yml" ] && cp "$LATEST_BACKUP/config.yml" plugins/ScoreboardPlugin/config.yml
[ -f "$LATEST_BACKUP/server.properties" ] && cp "$LATEST_BACKUP/server.properties" server.properties

echo "[INFO] Note: Checking if ~/mc-server-backup exists for world restore..."
ls -ld ~/mc-server-backup* 2>/dev/null || echo "[WARN] No ~/mc-server-backup* found; DB/config/seed restored, but deleted world folders cannot be recovered unless another backup exists."

./start_mc_server.sh
EOF

