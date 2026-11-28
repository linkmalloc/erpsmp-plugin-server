#!/bin/bash
# fix_bedrock_geyser.sh - Updates Geyser Standalone and NetherNet config on the remote server

set -e

SERVER="ubuntu@52.201.251.127"
KEY=".secret/erpsmp-dev.pem"

echo "[1/2] Uploading updated start_mc_server.sh to server..."
scp -i "$KEY" start_mc_server.sh "$SERVER:~/theerpdevs/"

echo "[2/2] Updating Geyser Standalone, configuring NetherNet, and restarting Geyser screen session..."
ssh -i "$KEY" "$SERVER" 'bash -s' << 'EOF'
set -e
cd ~/theerpdevs

# Ensure libssl3 is present for NetherNet WebRTC crypto
if ! dpkg -s libssl3 >/dev/null 2>&1; then
    echo "[INFO] Installing libssl3..."
    sudo apt-get update && sudo apt-get install -y libssl3
fi

# Open TCP & UDP 19132 in ufw if ufw is active
if sudo ufw status | grep -q "Status: active"; then
    echo "[INFO] Allowing 19132/tcp and 19132/udp in UFW..."
    sudo ufw allow 19132/tcp
    sudo ufw allow 19132/udp
fi

mkdir -p geyser-standalone
cd geyser-standalone

echo "[INFO] Downloading latest Geyser Standalone (NetherNet support)..."
curl -fsSL -o Geyser.jar.tmp "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/standalone"
if [ -s Geyser.jar.tmp ]; then
    mv Geyser.jar.tmp Geyser.jar
    echo "[SUCCESS] Updated Geyser.jar to the latest build!"
else
    rm -f Geyser.jar.tmp
    echo "[ERROR] Failed to download Geyser.jar"
    exit 1
fi

# Update config.yml for NetherNet if config.yml exists
if [ -f "config.yml" ]; then
    cp config.yml config.yml.bak
    python3 - << 'PYEOF'
import re

with open("config.yml", "r") as f:
    content = f.read()

# Ensure clone-remote-port is false
content = re.sub(r'(^\s*clone-remote-port:\s*).*$', r'\g<1>false', content, flags=re.MULTILINE)

# Add NetherNet settings under bedrock: if transport: nethernet is not already set
if "transport: nethernet" not in content:
    nethernet_block = """  transport: nethernet
  webrtc-port: 19132
  signaling:
    mode: builtin
  stun-discovery: false
  advertise-addresses:
    - "play.theerpsmp.net:19132"
    - "52.201.251.127:19132"
"""
    content = re.sub(r'(^\s*port:\s*19132\s*\n)', r'\1' + nethernet_block, content, count=1, flags=re.MULTILINE)

with open("config.yml", "w") as f:
    f.write(content)
print("[SUCCESS] Updated geyser-standalone/config.yml with NetherNet settings.")
PYEOF
fi

# Restart only the geyser screen session so online Java players are not disconnected
echo "[INFO] Restarting 'geyser' screen session..."
screen -ls | grep -E "[0-9]+\.geyser" | awk '{print $1}' | while read -r s; do
    screen -S "$s" -X quit || true
done
sleep 1
if pgrep -f "Geyser.jar" > /dev/null; then
    pkill -9 -f "Geyser.jar" || true
    sleep 1
fi

screen -dmS geyser bash -c "while true; do java -Xmx512M -Xms256M -DgeyserAdvertiseAddresses=52.201.251.127 -jar Geyser.jar --nogui; sleep 3; done"
sleep 3
screen -ls
echo "[SUCCESS] Geyser Standalone restarted!"
EOF

