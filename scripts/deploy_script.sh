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

# redis-cli 설치 확인 및 설치
if ! command -v redis-cli &> /dev/null; then
  echo "→ Installing redis-tools"
  sudo apt update
  sudo apt install redis-tools -y
fi

# ✨ .env.prod 파일 로드
if [ -f "$APP_DIR/.env.prod" ]; then
  echo "→ Loading environment variables from .env.prod"
  if [ -s "$APP_DIR/.env.prod" ]; then
    export $(grep -v '^#' "$APP_DIR/.env.prod" | xargs)
    echo "→ Environment variables loaded successfully"
    echo "→ REDIS_HOST=$REDIS_HOST"
    echo "→ REDIS_PORT=$REDIS_PORT"
  else
    echo "→ ERROR: .env.prod file is empty!"
    exit 1
  fi
else
  echo "→ ERROR: .env.prod file not found!"
  exit 1
fi

# 필수 환경 변수 확인
REQUIRED_VARS="DB_URL DB_USERNAME DB_PASSWORD REDIS_HOST REDIS_PORT"
for var in $REQUIRED_VARS; do
  if [ -z "${!var}" ]; then
    echo "→ ERROR: Required environment variable $var is missing!"
    exit 1
  fi
done

# Redis 연결 테스트 (TLS 고려)
echo "→ Testing Redis connection with TLS"
timeout 5 redis-cli --tls -h "$REDIS_HOST" -p "$REDIS_PORT" ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "→ Redis TLS connection successful"
else
  echo "→ WARNING: Failed to connect to Redis with TLS at $REDIS_HOST:$REDIS_PORT"
  # exit 1 # 테스트용으로 주석처리. 운영에서는 살려야 안정적
fi

echo "→ Starting new jar"
cd "$APP_DIR"
nohup java -jar "$JAR_NAME" \
  -Dspring.profiles.active=prod \
  -Dspring.data.redis.host="$REDIS_HOST" \
  -Dspring.data.redis.port="$REDIS_PORT" \
  -Dspring.data.redis.ssl.enabled=true \
  > "$LOG_FILE" 2>&1 &

echo "[$(date)] Deploy script finished"