#!/bin/bash

echo "[$(date)] Deploy script started"

APP_DIR="/home/$(whoami)/app"
JAR_NAME="$APP_DIR/app.jar"
LOG_FILE="$APP_DIR/logs/app.log"

echo "→ Creating app directory if not exists"
mkdir -p "$APP_DIR/logs"

echo "→ Killing old process"
PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
  kill -9 $PID
  echo "→ Old process killed (PID=$PID)"
else
  echo "→ No process to kill"
fi

# ✨ .env.prod 파일 로드
if [ -f "$APP_DIR/.env.prod" ]; then
  echo "→ Loading environment variables from .env.prod"
  if [ -s "$APP_DIR/.env.prod" ]; then
    export $(grep -v '^#' "$APP_DIR/.env.prod" | xargs)
    echo "→ DB_URL=$DB_URL"
    echo "→ DB_USERNAME=$DB_USERNAME"
    echo "→ DB_PASSWORD=$DB_PASSWORD"
    echo "→ REDIS_HOST=$REDIS_HOST"
    echo "→ REDIS_PORT=$REDIS_PORT"
    echo "→ REDIS_PASSWORD=$REDIS_PASSWORD"
  else
    echo "→ ERROR: .env.prod file is empty!"
    exit 1
  fi
else
  echo "→ ERROR: .env.prod file not found!"
  exit 1
fi

echo "→ Starting new jar"
cd "$APP_DIR"
nohup java -jar "$JAR_NAME" --spring.profiles.active=prod \
  -Dspring.datasource.url="$DB_URL" \
  -Dspring.datasource.username="$DB_USERNAME" \
  -Dspring.datasource.password="$DB_PASSWORD" \
  -Dspring.data.redis.host="$REDIS_HOST" \
  -Dspring.data.redis.port="$REDIS_PORT" \
  -Dspring.data.redis.password="$REDIS_PASSWORD" > "$LOG_FILE" 2>&1 &

echo "[$(date)] Deploy script finished"