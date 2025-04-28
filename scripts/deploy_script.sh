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
  echo "→ Old process found. Killing PID=$PID"
  kill -9 $PID
else
  echo "→ No old process found. Skipping kill."
fi

# redis-cli 설치 확인 및 설치
if ! command -v redis-cli &> /dev/null; then
  echo "→ redis-cli not found. Installing redis-tools..."
  sudo apt update
  sudo apt install redis-tools -y
else
  echo "→ redis-cli already installed."
fi

# ✨ .env.prod 파일 로드
if [ -f "$APP_DIR/.env.prod" ]; then
  echo "→ .env.prod file found. Loading environment variables..."
  if [ -s "$APP_DIR/.env.prod" ]; then
    export $(grep -v '^#' "$APP_DIR/.env.prod" | xargs)
    echo "→ Environment variables loaded successfully."
    echo "→ REDIS_HOST=$REDIS_HOST"
    echo "→ REDIS_PORT=$REDIS_PORT"
  else
    echo "❌ ERROR: .env.prod is empty!"
    exit 1
  fi
else
  echo "❌ ERROR: .env.prod not found!"
  exit 1
fi

# 필수 환경 변수 확인
REQUIRED_VARS="DB_URL DB_USERNAME DB_PASSWORD REDIS_HOST REDIS_PORT"
for var in $REQUIRED_VARS; do
  if [ -z "${!var}" ]; then
    echo "❌ ERROR: Required environment variable $var is missing!"
    exit 1
  fi
done

# Redis 연결 테스트 (TLS 고려)
echo "→ Testing Redis connection (with TLS)..."
timeout 5 redis-cli --tls -h "$REDIS_HOST" -p "$REDIS_PORT" ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "→ Redis TLS connection successful."
else
  echo "⚠️ WARNING: Failed to connect to Redis at $REDIS_HOST:$REDIS_PORT"
fi

# ✨ DB 연결 테스트 추가 (MySQL)
echo "→ Testing MySQL DB connection..."
timeout 5 mysql -h "$(echo $DB_URL | sed -E 's/^jdbc:mysql:\/\/([^:\/]+):?.*$/\1/')" -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "→ MySQL DB connection successful."
else
  echo "❌ ERROR: Failed to connect to MySQL DB."
  exit 1
fi

echo "→ Starting new JAR..."

cd "$APP_DIR"
nohup java -Ddebug -jar "$JAR_NAME" \
  -Dspring.profiles.active=prod \
  -Dspring.data.redis.host="$REDIS_HOST" \
  -Dspring.data.redis.port="$REDIS_PORT" \
  -Dspring.data.redis.ssl.enabled=true \
  > "$LOG_FILE" 2>&1 &

echo "[$(date)] Deploy script finished"
echo "→ Waiting 5 seconds for logs..."
sleep 5

echo "→ Showing last 100 lines of log:"
tail -n 100 "$LOG_FILE"
