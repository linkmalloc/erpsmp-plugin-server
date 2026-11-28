#!/bin/bash
set -e

SERVER="ubuntu@52.201.251.127"
KEY=".secret/erpsmp-dev.pem"

echo "WARNING: This will clear everyone's custom Ender Chest EXCEPT Merp208 and restart the server."
read -p "Type CLEAR to continue: " CONFIRM
if [ "$CONFIRM" != "CLEAR" ]; then
    echo "Aborted."
    exit 1
fi

ssh -i "$KEY" "$SERVER" 'bash -s' << 'EOF'
set -e

cd ~/mc-server

echo "[1/4] Stopping Minecraft & Geyser so in-memory Ender Chests don't overwrite the DB..."
pkill -9 -f "start.sh" >/dev/null 2>&1 || true
screen -wipe >/dev/null 2>&1 || true
screen -ls | grep -E "[0-9]+\.(minecraft|geyser)" | awk '{print $1}' | while read -r s; do
    screen -S "$s" -X quit || true
done
pkill -9 -f "paper.jar" >/dev/null 2>&1 || true
pkill -9 -f "Geyser.jar" >/dev/null 2>&1 || true
sleep 2

echo "[2/4] Backing up playerdata.db..."
BACKUP_DIR=~/ec-backup-$(date +%Y%m%d-%H%M%S)
mkdir -p "$BACKUP_DIR"
[ -f plugins/ScoreboardPlugin/playerdata.db ] && cp plugins/ScoreboardPlugin/playerdata.db* "$BACKUP_DIR/" 2>/dev/null || true

echo "[3/4] Clearing all custom Ender Chests except Merp208..."
python3 - << 'PYEOF'
import os, sqlite3

MERP_UUID = "1ba04e2c-6696-3083-8059-c94736a7d303"
db_path = "plugins/ScoreboardPlugin/playerdata.db"
if os.path.exists(db_path):
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute(
        """
        UPDATE player_stats
        SET enderChest = NULL
        WHERE LOWER(uuid) != ?
          AND LOWER(COALESCE(lastKnownName, '')) != 'merp208'
        """,
        (MERP_UUID.lower(),),
    )
    cleared = cur.rowcount
    conn.commit()
    cur.execute("PRAGMA wal_checkpoint(TRUNCATE);")
    conn.close()
    print(f"[INFO] Cleared custom Ender Chest for {cleared} players (kept Merp208).")
else:
    print("[WARN] playerdata.db not found!")
PYEOF

echo "[4/4] Restarting Minecraft & Geyser..."
./start_mc_server.sh
EOF

echo "[DONE] Cleared everyone's custom Ender Chest except Merp208!"

