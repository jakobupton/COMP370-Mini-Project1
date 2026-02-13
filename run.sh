#!/bin/bash

# Compile Java files
echo "Compiling Java files..."
javac -d out src/main/java/comp370/srms/*.java

if [ $? -ne 0 ]; then
  echo "Compilation failed."
  exit 1
fi

# Cleanup function
cleanup() {
  echo ""
  echo "Shutting down all processes..."

  kill $MONITOR_PID 2>/dev/null
  kill $PROC1_PID 2>/dev/null
  kill $PROC2_PID 2>/dev/null
  kill $PROC3_PID 2>/dev/null
  kill $CLIENT_PID 2>/dev/null

  rm -f primary.pid
  wait 2>/dev/null
  echo "All processes terminated."
  exit 0
}

# Trap Ctrl+C and script termination
trap cleanup SIGINT SIGTERM EXIT

echo "Starting ServerMonitor..."
java -cp out comp370.srms.ServerMonitor &
MONITOR_PID=$!

sleep 1

echo "Starting three ServerProcess instances..."

# Storing PID of primary for kill-primary.sh
java -cp ./out comp370.srms.ServerProcess &
PRIMARY_PID=$!
echo $PRIMARY_PID > primary.pid
echo "Primary ServerProcess PID: $PRIMARY_PID"

# Wait one second to make sure the first server becomes the primary
sleep 1

java -cp out comp370.srms.ServerProcess &
PROC2_PID=$!

java -cp out comp370.srms.ServerProcess &
PROC3_PID=$!

sleep 1

echo "Starting Client..."
java -cp out comp370.srms.Client &
CLIENT_PID=$!

echo "All components started"
wait
