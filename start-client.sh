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
  echo "Stopping Client..."
  kill $CLIENT_PID 2>/dev/null
  wait 2>/dev/null
  echo "Client stopped."
  exit 0
}

trap cleanup SIGINT SIGTERM EXIT

echo "Starting Client..."
java -cp out comp370.srms.Client &
CLIENT_PID=$!

echo "Client running (PID: $CLIENT_PID)"
wait
