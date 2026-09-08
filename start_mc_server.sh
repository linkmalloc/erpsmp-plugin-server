#!/bin/bash
# start_mc_server.sh - Wrapper to safely restart the Minecraft server screen session

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}[INFO] Stopping any existing Minecraft server instances...${NC}"

# 1. Kill any background start.sh watchdog loops first to prevent respawns
pkill -9 -f "start.sh" > /dev/null 2>&1

# 2. Terminate all screen sessions matching minecraft
screen -ls | grep -E "[0-9]+\.minecraft" | awk '{print $1}' | while read -r s; do
    echo -e "${YELLOW}[INFO] Terminating screen session $s...${NC}"
    screen -S "$s" -X quit
done
sleep 2

# 3. Force kill any orphaned Java processes running paper.jar
if pgrep -f "paper.jar" > /dev/null; then
    echo -e "${YELLOW}[INFO] Found orphaned Java processes. Force killing them...${NC}"
    pkill -9 -f "paper.jar"
    sleep 1
fi

# 3. Clean up lock files to prevent "session.lock" issues
echo -e "${BLUE}[INFO] Cleaning up session.lock files...${NC}"
# Search in current directory and its subdirectories (like world/session.lock)
find . -name "session.lock" -type f -print -delete

echo -e "${GREEN}[SUCCESS] Environment cleaned up and ready for a fresh start!${NC}"
echo -e "${BLUE}[INFO] Starting Minecraft watchdog loop inside screen session 'minecraft'...${NC}"
echo -e "${YELLOW}[NOTE] Detach with: Ctrl+A, then D${NC}"
echo -e "${YELLOW}[NOTE] Reattach with: screen -r minecraft${NC}"
echo ""

# Start screen session
screen -S minecraft ./start.sh
