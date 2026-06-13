#!/bin/bash
set -e

PROJECT_DIR="/mnt/c/Users/bhara/Downloads/backend-trade-file/backend-trade-file/tradestreamengine/tradestreamengine/trade-file-processing"

echo ""
echo "=============================================="
echo "  TradeStreamEngine — Backend Startup"
echo "=============================================="
echo ""

cd "$PROJECT_DIR"

echo "[1/4] Stopping old containers..."
docker compose down -v --remove-orphans 2>/dev/null || true

echo ""
echo "[2/4] Building Docker image..."
docker compose build --no-cache

echo ""
echo "[3/4] Starting PostgreSQL + Backend..."
docker compose up -d

echo ""
echo "[4/4] Waiting for backend to start..."
for i in $(seq 1 24); do
  sleep 5
  if docker logs trade-backend-service 2>&1 | grep -q "Started TradeStreamMain"; then
    echo ""
    echo "=============================================="
    echo "  Backend is UP!"
    echo "  API:     http://localhost:8080"
    echo "  Health:  http://localhost:8080/actuator/health"
    echo "=============================================="
    echo ""
    exit 0
  fi
  if docker logs trade-backend-service 2>&1 | grep -q "Application run failed"; then
    echo ""
    echo "FAILED. Full logs:"
    docker logs trade-backend-service 2>&1
    exit 1
  fi
  echo "  Waiting... ($((i*5))s)"
done

echo "Timeout. Logs:"
docker logs trade-backend-service 2>&1
