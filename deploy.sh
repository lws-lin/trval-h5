#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v docker >/dev/null 2>&1; then
  echo "请先安装并启动 Docker"
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  echo "请先安装 Docker Compose"
  exit 1
fi

case "${1:-up}" in
  up)
    "${COMPOSE[@]}" up -d --build
    echo "已启动。浏览器打开 http://127.0.0.1${PORT:+:$PORT}"
    ;;
  down)
    "${COMPOSE[@]}" down
    ;;
  logs)
    "${COMPOSE[@]}" logs -f
    ;;
  *)
    echo "用法: ./deploy.sh [up|down|logs]"
    exit 1
    ;;
esac
