#!/bin/bash
# start.sh - Script to run the Minecraft server 24/7 and log crashes.

# File where crash/stop logs will be written
LOG_FILE="server_run.log"

echo "Starting Minecraft Server Watchdog loop..."
echo "All outputs will be logged to $LOG_FILE"

while true; do
    echo "==========================================================" >> "$LOG_FILE"
    echo "SERVER STARTING: $(date)" >> "$LOG_FILE"
    echo "==========================================================" >> "$LOG_FILE"

    # Start the server, redirecting both stdout and stderr to tee which prints to terminal and appends to the log file.
    java -Xmx3G -Xms3G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8m -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -jar paper.jar nogui 2>&1 | tee -a "$LOG_FILE"
    
    EXIT_CODE=${PIPESTATUS[0]} # Captures the exit code of the java command instead of tee

    echo "==========================================================" >> "$LOG_FILE"
    echo "SERVER STOPPED: $(date)" >> "$LOG_FILE"
    echo "Exit Code: $EXIT_CODE" >> "$LOG_FILE"
    echo "==========================================================" >> "$LOG_FILE"

    echo "Server closed with exit code $EXIT_CODE."
    echo "Restarting in 5 seconds... Press Ctrl+C to stop the loop."
    sleep 5
done
