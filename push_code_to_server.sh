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
scp -i "$KEY" start_mc_server.sh "$SERVER:~/theerpdevs/"

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

cd ~/theerpdevs
chmod +x start_mc_server.sh start.sh 2>/dev/null || true
screen -wipe >/dev/null 2>&1 || true

# Locate Geyser jar(s) and config(s) on the server
echo "[INFO] Locating Geyser installation..."
find ~ -maxdepth 4 -iname "*geyser*.jar" 2>/dev/null || true

# Update Geyser Spigot plugin if present
if [ -f "plugins/Geyser-Spigot.jar" ]; then
    echo "[INFO] Updating plugins/Geyser-Spigot.jar to latest build..."
    curl -fsSL -o plugins/Geyser-Spigot.jar.tmp "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" && mv plugins/Geyser-Spigot.jar.tmp plugins/Geyser-Spigot.jar
fi

# Update any Geyser config.yml (plugin or standalone)
for CFG in plugins/Geyser-Spigot/config.yml geyser-standalone/config.yml ~/geyser-standalone/config.yml ~/geyser/config.yml; do
    if [ -f "$CFG" ]; then
        echo "[INFO] Configuring NetherNet in $CFG..."
        cp "$CFG" "${CFG}.bak"
        python3 - "$CFG" << 'PYEOF'
import re, sys
path = sys.argv[1]
with open(path, "r") as f:
    content = f.read()

for pat in [
    r'^\s*webrtc-port:.*\n',
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
done

echo "[3/4] Building ScoreboardPlugin on server..."
cd ~/theerpdevs/money
mvn clean package -DskipTests
cp target/ScoreboardPlugin-1.0-SNAPSHOT.jar ~/theerpdevs/plugins/

echo "[4/4] Restarting Minecraft & Geyser servers..."
cd ~/theerpdevs
./start_mc_server.sh

echo "[INFO] Waiting up to 30s for Paper (25565) and Geyser (19132) to bind ports..."
for i in $(seq 1 15); do
    sleep 2
    if ss -tulpn 2>/dev/null | grep -q ":19132"; then
        break
    fi
done

ss -tulpn 2>/dev/null | grep -E ":(25565|19132)" || true
if ! ss -tulpn 2>/dev/null | grep -q ":19132"; then
    echo "[ERROR] Port 19132 is still not listening! Diagnostics:"
    screen -ls || true
    ps aux | grep -i java | grep -v grep || true
    echo "--- Last 35 lines of ~/theerpdevs/logs/latest.log ---"
    tail -n 35 ~/theerpdevs/logs/latest.log 2>/dev/null || true
    exit 1
fi
EOF

echo "[INFO] Verifying external TCP 19132 reachability..."
if nc -zv -G 4 18.206.179.9 19132 >/dev/null 2>&1; then
    echo "[SUCCESS] TCP port 19132 is open and reachable on 18.206.179.9! Bedrock players can now join."
else
    echo "[WARN] Geyser started locally, but TCP 19132 could not be reached externally."
fi
