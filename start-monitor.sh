#!/bin/bash

# Compile Java files
echo "Compiling Java files..."
javac -d out src/main/java/comp370/srms/*.java

if [ $? -ne 0 ]; then
  echo "Compilation failed."
  exit 1
fi

cleanup() {
  echo ""
  echo "Stopping ServerMonitor..."
  kill $MONITOR_PID 2>/dev/null
  wait 2>/dev/null
  echo "ServerMonitor stopped."
  exit 0
}

trap cleanup SIGINT SIGTERM EXIT

echo "Starting ServerMonitor..."
java -cp out comp370.srms.ServerMonitor &
MONITOR_PID=$!

echo "ServerMonitor running (PID: $MONITOR_PID)"
wait
