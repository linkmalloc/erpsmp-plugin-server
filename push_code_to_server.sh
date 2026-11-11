#!/bin/bash
set -e

SERVER="ubuntu@18.206.179.9"
KEY=".secret/erpsmp-dev.pem"

echo "[1/3] Uploading plugin source and startup script to server..."
scp -r -i "$KEY" plugins/money "$SERVER:~/theerpdevs/"
scp -i "$KEY" start_mc_server.sh "$SERVER:~/theerpdevs/"

echo "[2/3] Building ScoreboardPlugin on server and updating Geyser config..."
ssh -i "$KEY" "$SERVER" 'bash -s' << 'EOF'
set -e
cd ~/theerpdevs/money
mvn clean package -DskipTests
cp target/ScoreboardPlugin-1.0-SNAPSHOT.jar ~/theerpdevs/plugins/

cd ~/theerpdevs
chmod +x start_mc_server.sh

# Ensure Geyser NetherNet settings are configured if geyser-standalone/config.yml exists
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
    - "play.theerpsmp.net:19132"
    - "18.206.179.9:19132"
"""
    content = re.sub(r'(^\s*port:\s*19132\s*\n)', r'\1' + nethernet_block, content, count=1, flags=re.MULTILINE)
with open("geyser-standalone/config.yml", "w") as f:
    f.write(content)
PYEOF
fi

echo "[3/3] Restarting Minecraft & Geyser servers..."
./start_mc_server.sh
EOF
