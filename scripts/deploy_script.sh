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

# ✨ 여기 추가: 환경변수 export
echo "→ Exporting environment variables"
export REDIS_HOST=${REDIS_HOST}
export REDIS_PASSWORD=${REDIS_PASSWORD}
export DB_URL=${DB_URL}
export DB_USERNAME=${DB_USERNAME}
export DB_PASSWORD=${DB_PASSWORD}
export JWT_SECRET=${JWT_SECRET}

# (필요한 환경변수 다 export 해줘야 해.)

echo "→ Starting new jar"
cd "$APP_DIR"
nohup java -jar "$JAR_NAME" --spring.profiles.active=prod > "$LOG_FILE" 2>&1 &

echo "[$(date)] Deploy script finished"
