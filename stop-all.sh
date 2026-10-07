#!/usr/bin/env bash

echo "Stopping all services on ports 8081-8084..."
for port in 8081 8082 8083 8084; do
    pid=$(lsof -ti :$port)
    if [ -n "$pid" ]; then
        while IFS= read -r process_id; do
            echo "Stopping process on port $port (PID: $process_id)"
            kill "$process_id"
            if kill -0 "$process_id" 2>/dev/null; then
                echo "Force stopping process on port $port (PID: $process_id)"
                kill -9 "$process_id"
            fi
        done <<EOF
$pid
EOF
    fi
done
echo "Done."

# Attempt to stop Kafka via docker compose (best-effort)
KAFKA_COMPOSE_FILE="$(cd "$(dirname "$0")" && pwd)/kafka-compose.yml"
if [ -f "$KAFKA_COMPOSE_FILE" ]; then
  echo "Stopping Kafka (docker compose)..."
  if docker compose -f "$KAFKA_COMPOSE_FILE" down >/dev/null 2>&1; then
    echo "Kafka stopped via docker compose."
  else
    echo "Failed to stop Kafka (docker compose failed or docker not running)."
  fi
else
  echo "Kafka compose file not found at $KAFKA_COMPOSE_FILE. Skipping Kafka stop."
fi
