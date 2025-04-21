#!/bin/bash
set -e  # 오류 발생 시 즉시 중단

APP_DIR="/home/$EC2_USER/app"
JAR_NAME="quiz-0.0.1-SNAPSHOT.jar"

echo "[$(date)] Deploy script started"

# 1) 작업 디렉터리로 이동 (디렉토리 없으면 생성)
echo "→ Creating app directory if not exists"
sudo mkdir -p "$APP_DIR"
sudo chown -R "$EC2_USER:$EC2_USER" "$APP_DIR"

cd "$APP_DIR"

# 2) 기존 프로세스 종료
echo "→ Killing old process"
OLD_PID=$(pgrep -f "java -jar $JAR_NAME" || true)
if [ -n "$OLD_PID" ]; then
  kill -9 $OLD_PID
  echo "   Killed PID $OLD_PID"
fi

# 3) 새 프로세스 기동
echo "→ Starting new jar"
nohup java -jar $JAR_NAME --spring.profiles.active=prod > app.log 2>&1 &

echo "[$(date)] Deploy script finished"
