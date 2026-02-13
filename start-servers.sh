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
  echo "Stopping all ServerProcess instances..."

  kill $PRIMARY_PID 2>/dev/null
  kill $PROC2_PID 2>/dev/null
  kill $PROC3_PID 2>/dev/null

  rm -f primary.pid
  wait 2>/dev/null
  echo "All ServerProcesses stopped."
  exit 0
}

trap cleanup SIGINT SIGTERM EXIT

echo "Starting three ServerProcess instances..."

# Start primary
java -cp out comp370.srms.ServerProcess &
PRIMARY_PID=$!
echo $PRIMARY_PID > primary.pid
echo "Primary ServerProcess PID: $PRIMARY_PID"

# Ensure first becomes primary
sleep 1

# Start backups
java -cp out comp370.srms.ServerProcess &
PROC2_PID=$!

java -cp out comp370.srms.ServerProcess &
PROC3_PID=$!

echo "All ServerProcess instances started."
wait
