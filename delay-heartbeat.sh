#!/bin/bash

DELAY="3000ms"

apply_delay() {
  echo "Applying $DELAY artificial delay to loopback..."
  sudo tc qdisc add dev lo root netem delay $DELAY 2>/dev/null

  if [ $? -ne 0 ]; then
    echo "Delay may already be applied. Attempting to replace..."
    sudo tc qdisc replace dev lo root netem delay $DELAY
  fi
}

remove_delay() {
  echo "Removing artificial delay..."
  sudo tc qdisc del dev lo root netem 2>/dev/null
  echo "Network restored."
}

# Ensure cleanup on exit or Ctrl+C
trap remove_delay EXIT SIGINT SIGTERM

apply_delay

echo "Delay active for 3 seconds..."
sleep 3

echo "Time elapsed."

# Cleanup handled automatically by trap
