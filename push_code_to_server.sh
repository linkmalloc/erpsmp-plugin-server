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
chmod +x start_mc_server.sh

# Ensure Geyser NetherNet settings are configured in geyser-standalone/config.yml
if [ -f "geyser-standalone/config.yml" ]; then
    python3 - << 'PYEOF'
import re
with open("geyser-standalone/config.yml", "r") as f:
    content = f.read()
content = re.sub(r'(^\s*clone-remote-port:\s*).*$', r'\g<1>false', content, flags=re.MULTILINE)
if "transport: nethernet" not in content:
    nethernet_block = """  transport: nethernet
  webrtc-port: 19132
  signaling:
    mode: builtin
  stun-discovery: false
  advertise-addresses:
    - "18.206.179.9:19132"
    - "play.theerpsmp.net:19132"
"""
    content = re.sub(r'(^\s*port:\s*19132\s*\n)', r'\1' + nethernet_block, content, count=1, flags=re.MULTILINE)
with open("geyser-standalone/config.yml", "w") as f:
    f.write(content)
PYEOF
fi

echo "[3/4] Building ScoreboardPlugin on server..."
cd ~/theerpdevs/money
mvn clean package -DskipTests
cp target/ScoreboardPlugin-1.0-SNAPSHOT.jar ~/theerpdevs/plugins/

echo "[4/4] Restarting Minecraft & Geyser servers..."
cd ~/theerpdevs
./start_mc_server.sh
EOF

echo "[INFO] Waiting 6 seconds for Geyser NetherNet to bind port 19132..."
sleep 6
if nc -zv -G 4 18.206.179.9 19132 >/dev/null 2>&1; then
    echo "[SUCCESS] TCP port 19132 is open and reachable on 18.206.179.9! Bedrock players can now join."
else
    echo "[ACTION REQUIRED] Geyser is restarted, but TCP port 19132 is still blocked externally by AWS."
    echo "Please open your AWS EC2 Console -> Security Groups -> Inbound Rules for 18.206.179.9"
    echo "and add: Type = Custom TCP, Port = 19132, Source = 0.0.0.0/0 (alongside UDP 19132)."
fi
