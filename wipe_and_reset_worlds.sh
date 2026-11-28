#!/bin/bash
set -e

SERVER="ubuntu@52.201.251.127"
KEY=".secret/erpsmp-dev.pem"

echo "WARNING: This will stop the server, wipe all player inventories/armor/stats/ender chests/Erpies/Derpies/bank,"
echo "and delete the overworld, nether, and end worlds with a new random seed (keeping afk, afk_zone, spawn, and spawn2 intact)."
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

echo "[2/5] Clearing custom Ender Chests, Erpies/Derpies, Bank, and plugin stats in playerdata.db..."
python3 - << 'PYEOF'
import os, sqlite3, re

db_path = "plugins/ScoreboardPlugin/playerdata.db"
if os.path.exists(db_path):
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("""
        UPDATE player_stats SET
            timePlayed = 0,
            erpies = 0,
            derpies = 0,
            bankErpies = 0,
            bankDerpies = 0,
            lastInterestTime = 0,
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
            enderChest = NULL,
            bankItems = NULL
    """)
    updated = cur.rowcount
    conn.commit()
    cur.execute("PRAGMA wal_checkpoint(TRUNCATE);")
    conn.close()
    print(f"[INFO] Wiped stats, enderChest, Erpies/Derpies, and bank for {updated} players in SQLite.")

cfg_path = "plugins/ScoreboardPlugin/config.yml"
if os.path.exists(cfg_path):
    with open(cfg_path, "r") as f:
        cfg = f.read()
    for key in ["enderChest", "bankItems"]:
        cfg = re.sub(rf'^\s*{key}:\s*.*\n(?:\s{{6,}}.*\n)*', '', cfg, flags=re.MULTILINE)
    for key in ["erpies", "derpies", "bankErpies", "bankDerpies", "lastInterestTime", "kills", "deaths", "timePlayed"]:
        cfg = re.sub(rf'^(\s*{key}:\s*).*$', r'\g<1>0', cfg, flags=re.MULTILINE)
    with open(cfg_path, "w") as f:
        f.write(cfg)
PYEOF

echo "[3/5] Deleting overworld, nether, and end (keeping afk, afk_zone, spawn, and spawn2 untouched)..."
for W in "$LEVEL_NAME" "${LEVEL_NAME}_nether" "${LEVEL_NAME}_the_end" world world_nether world_the_end; do
    case "$W" in
        afk|afk_zone|spawn|spawn2)
            echo "[INFO] Skipping protected world: $W"
            ;;
        *)
            [ -d "$W" ] && echo "[INFO] Removing $W" && rm -rf "$W"
            ;;
    esac
done

for d in plugins/Multiverse-Inventories/players plugins/Multiverse-Inventories/worlds; do
    [ -d "$d" ] && echo "[INFO] Removing $d" && rm -rf "$d"
done

echo "[4/5] Resetting level-seed in server.properties for a brand-new random seed..."
if [ -f server.properties ]; then
    sed -i 's/^level-seed=.*/level-seed=/' server.properties
fi

echo "[5/5] Starting Minecraft & Geyser with fresh worlds..."
./start_mc_server.sh
EOF

echo "[DONE] Overworld/Nether/End and playerdata/stats/armor/ender chests/Erpies/bank reset (afk, spawn, spawn2 kept intact)."
