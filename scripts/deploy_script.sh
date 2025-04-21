#!/bin/bash

echo "[$(date)] Deploy script started"

APP_DIR="/home/$(whoami)/app"
JAR_NAME="$APP_DIR/app.jar"
LOG_FILE="$APP_DIR/logs/app.log"

echo "→ Creating app directory if not exists"
mkdir -p "$APP_DIR/logs"

echo "→ Loading environment variables from .env.prod"
if [ -f "$APP_DIR/.env.prod" ]; then
  export $(grep -v '^#' "$APP_DIR/.env.prod" | xargs)
else
  echo "⚠️ .env.prod not found in $APP_DIR"
fi

echo "→ Killing old process"
PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
  kill -9 $PID
  echo "→ Old process killed (PID=$PID)"
else
  echo "→ No process to kill"
fi

echo "→ Starting new jar"
cd "$APP_DIR"
nohup java -jar "$JAR_NAME" --spring.profiles.active=prod > "$LOG_FILE" 2>&1 &

echo "[$(date)] Deploy script finished"
