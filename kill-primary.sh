#!/bin/bash

if [ ! -f primary.pid ]; then
  echo "primary.pid file not found."
  echo "Primary may not be running."
  exit 1
fi

PID=$(cat primary.pid)

if ps -p $PID > /dev/null 2>&1; then
  echo "Killing primary ServerProcess (PID: $PID)..."
  kill $PID
  rm -f primary.pid
  echo "Primary terminated."
else
  echo "Process $PID not running."
  rm -f primary.pid
fi
