#!/bin/bash
set -e

SERVER="ubuntu@18.206.179.9"
KEY=".secret/erpsmp-dev.pem"
CS_FILE="plugins/money/src/main/java/net/theerpsmp/plugin/CustomScoreboard.java"

# Guard against IDE diff-preview truncation overwriting CustomScoreboard.java
if grep -q "truncated for diff preview" "$CS_FILE" 2>/dev/null || [ "$(wc -l < "$CS_FILE")" -lt 18000 ]; then
    echo "[INFO] Restoring full CustomScoreboard.java from git HEAD before upload..."
    git show HEAD:"$CS_FILE" > "$CS_FILE"
fi

echo "[1/4] Uploading plugin source and startup script to server..."
scp -r -i "$KEY" plugins/money "$SERVER:~/theerpdevs/"
scp -i "$KEY" start_mc_server.sh "$SERVER:~/mc-server/"

echo "[2/4] Updating Geyser NetherNet config, dependencies, and firewall on server..."
ssh -i "$KEY" "$SERVER" 'bash -s' << 'EOF'
set -e

# Ensure OpenSSL 3 is installed (required by Geyser NetherNet native library)
if ! dpkg -s libssl3 >/dev/null 2>&1 && ! dpkg -s libssl3t64 >/dev/null 2>&1; then
    sudo apt-get update -y && (sudo apt-get install -y libssl3 || sudo apt-get install -y libssl3t64) || true
fi

# Ensure host firewall allows both TCP (NetherNet signaling) and UDP (WebRTC gameplay) on 19132
if command -v ufw >/dev/null 2>&1 && sudo ufw status | grep -q "Status: active"; then
    sudo ufw allow 19132/tcp >/dev/null 2>&1 || true
    sudo ufw allow 19132/udp >/dev/null 2>&1 || true
fi

cd ~/mc-server
chmod +x start_mc_server.sh start.sh 2>/dev/null || true
screen -wipe >/dev/null 2>&1 || true

# Prevent plugins/Geyser.jar from conflicting with geyser-standalone (or failing if overwritten with standalone jar)
if [ -d "geyser-standalone" ] && [ -f "plugins/Geyser.jar" ]; then
    mkdir -p ~/geyser-backup
    echo "[INFO] Moving plugins/Geyser.jar to ~/geyser-backup/ so geyser-standalone owns port 19132 cleanly..."
    mv plugins/Geyser.jar ~/geyser-backup/Geyser.jar.plugin.$(date +%s)
fi

# Configure NetherNet in ~/mc-server/geyser-standalone/config.yml
CFG="geyser-standalone/config.yml"
if [ -f "$CFG" ]; then
    echo "[INFO] Configuring NetherNet in: ~/mc-server/$CFG"
    cp "$CFG" "${CFG}.bak"
    python3 - "$CFG" << 'PYEOF'
import re, sys
path = sys.argv[1]
with open(path, "r") as f:
    content = f.read()

for pat in [
    r'^\s*signaling:\s*\n(?:\s+mode:\s*builtin\s*\n)?',
    r'^\s*stun-discovery:.*\n',
    r'^\s*advertise-addresses:\s*\n(?:\s+-\s*"[^"]*"\s*\n)*',
    r'^\s*transport:.*\n',
]:
    content = re.sub(pat, '', content, flags=re.MULTILINE)

content = re.sub(r'(^\s*clone-remote-port:\s*).*$', r'\g<1>false', content, flags=re.MULTILINE)
content = re.sub(r'(^\s*port:\s*19132\s*\n)', r'\1  transport: nethernet\n', content, count=1, flags=re.MULTILINE)

with open(path, "w") as f:
    f.write(content)
PYEOF
fi

echo "[3/4] Building ScoreboardPlugin on server..."
cd ~/theerpdevs/money
mvn clean package -DskipTests
cp target/ScoreboardPlugin-1.0-SNAPSHOT.jar ~/mc-server/plugins/

echo "[4/4] Restarting Minecraft & Geyser servers in ~/mc-server..."
cd ~/mc-server
./start_mc_server.sh

echo "[INFO] Waiting up to 35s for Paper (25565) and Geyser TCP+UDP (19132)..."
for i in $(seq 1 18); do
    sleep 2
    if ss -tlpn 2>/dev/null | grep -q ":19132"; then
        break
    fi
done

echo "[INFO] Ports currently listening:"
ss -tulpn 2>/dev/null | grep -E ":(25565|19132)" || true

if ! ss -tlpn 2>/dev/null | grep -q ":19132"; then
    echo "[ERROR] TCP 19132 is NOT listening!"
    echo "--- ~/mc-server/geyser-standalone/logs/latest.log ---"
    tail -n 35 ~/mc-server/geyser-standalone/logs/latest.log 2>/dev/null || true
    exit 1
fi
EOF

echo "[INFO] Verifying external TCP 19132 reachability..."
if nc -zv -G 4 18.206.179.9 19132 >/dev/null 2>&1; then
    echo "[SUCCESS] TCP port 19132 is open and reachable on 18.206.179.9! Bedrock players can now join."
else
    echo "[WARN] Geyser bound TCP 19132 locally, but external TCP 19132 failed."
fi
