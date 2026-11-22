#!/bin/bash
set -e

SERVER="ubuntu@18.206.179.9"
KEY=".secret/erpsmp-dev.pem"

echo "WARNING: This will stop the server, wipe all player inventories/armor/stats/ender chests,"
echo "and delete the active overworld, nether, and end worlds with a new random seed."
read -p "Type WIPE to continue: " CONFIRM
if [ "$CONFIRM" != "WIPE" ]; then
    echo "Aborted."
    exit 1
fi

ssh -i "$KEY" "$SERVER" 'bash -s' << 'EOF'
set -e

cd ~/mc-server

echo "[1/5] Stopping Minecraft & Geyser..."
pkill -9 -f "start.sh" >/dev/null 2>&1 || true
screen -wipe >/dev/null 2>&1 || true
screen -ls | grep -E "[0-9]+\.(minecraft|geyser)" | awk '{print $1}' | while read -r s; do
    screen -S "$s" -X quit || true
done
pkill -9 -f "paper.jar" >/dev/null 2>&1 || true
pkill -9 -f "Geyser.jar" >/dev/null 2>&1 || true
sleep 2

LEVEL_NAME=$(grep -E '^level-name=' server.properties 2>/dev/null | cut -d'=' -f2 | tr -d '\r' || true)
[ -z "$LEVEL_NAME" ] && LEVEL_NAME="world"
echo "[INFO] Active level-name in server.properties: $LEVEL_NAME"

BACKUP_DIR=~/wipe-backup-$(date +%Y%m%d-%H%M%S)
mkdir -p "$BACKUP_DIR"
[ -f plugins/ScoreboardPlugin/playerdata.db ] && cp plugins/ScoreboardPlugin/playerdata.db* "$BACKUP_DIR/" 2>/dev/null || true
[ -f plugins/ScoreboardPlugin/config.yml ] && cp plugins/ScoreboardPlugin/config.yml "$BACKUP_DIR/"
[ -f server.properties ] && cp server.properties "$BACKUP_DIR/"
echo "[INFO] Saved DB/config backup to $BACKUP_DIR"

echo "[2/5] Clearing custom Ender Chests and plugin stats in playerdata.db..."
python3 - << 'PYEOF'
import os, sqlite3, re

db_path = "plugins/ScoreboardPlugin/playerdata.db"
if os.path.exists(db_path):
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("""
        UPDATE player_stats SET
            timePlayed = 0,
            kills = 0,
            deaths = 0,
            killedAdmin = 0,
            killedDragon = 0,
            oresMined = 0,
            invisibleKills = 0,
            blocksPlaced = 0,
            starvationDeaths = 0,
            apocalypseZombieKills = 0,
            apocalypseLongestSurvival = 0,
            apocalypseMaxWavesSurvived = 0,
            foodsEaten = NULL,
            enderChest = NULL
    """)
    updated = cur.rowcount
    conn.commit()
    cur.execute("PRAGMA wal_checkpoint(TRUNCATE);")
    conn.close()
    print(f"[INFO] Cleared enderChest and stats for {updated} players in SQLite.")

cfg_path = "plugins/ScoreboardPlugin/config.yml"
if os.path.exists(cfg_path):
    with open(cfg_path, "r") as f:
        cfg = f.read()
    cfg = re.sub(r'^\s*enderChest:\s*.*\n(?:\s{6,}.*\n)*', '', cfg, flags=re.MULTILINE)
    with open(cfg_path, "w") as f:
        f.write(cfg)
PYEOF

echo "[3/5] Deleting $LEVEL_NAME, ${LEVEL_NAME}_nether, ${LEVEL_NAME}_the_end, and any playerdata/stats/advancements in other worlds..."
rm -rf "$LEVEL_NAME" "${LEVEL_NAME}_nether" "${LEVEL_NAME}_the_end"
rm -rf world world_nether world_the_end
for d in */playerdata */stats */advancements; do
    [ -d "$d" ] && echo "[INFO] Removing $d" && rm -rf "$d"
done

echo "[4/5] Resetting level-seed in server.properties for a brand-new random seed..."
if [ -f server.properties ]; then
    sed -i 's/^level-seed=.*/level-seed=/' server.properties
fi

echo "[5/5] Starting Minecraft & Geyser with fresh worlds..."
./start_mc_server.sh
EOF

echo "[DONE] Active worlds ($LEVEL_NAME) and all playerdata/stats/armor/ender chests have been reset! Give Paper ~30s to generate the new worlds."
