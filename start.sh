#!/bin/bash
# start.sh - Script to run the Minecraft server 24/7.

echo "Starting Minecraft Server Watchdog loop..."

while true; do
    # Start the server, outputting directly to the terminal.
    java -Xmx2048M -Xms512M -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8m -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -jar paper.jar nogui
    
    EXIT_CODE=$?

    echo "Server closed with exit code $EXIT_CODE."
    echo "Restarting in 5 seconds... Press Ctrl+C to stop the loop."
    sleep 5
done
