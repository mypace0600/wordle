#!/bin/bash
set -e  # 오류 발생 시 즉시 중단

APP_DIR="/home/${EC2_USER}/app"  # EC2_USER 환경 변수를 사용하여 디렉토리 경로 설정
JAR_NAME="quiz-application.jar"  # QuizApplication에 맞춰 JAR 파일 이름 설정

echo "[$(date)] Deploy script started"

# 1) 작업 디렉토리가 없으면 생성
echo "→ Creating app directory if not exists"
mkdir -p "$APP_DIR"  # 디렉토리가 없으면 생성

# 2) 작업 디렉토리로 이동
cd "$APP_DIR"

# 3) 기존 프로세스 종료
echo "→ Killing old process"
OLD_PID=$(pgrep -f "java -jar $JAR_NAME" || true)
if [ -n "$OLD_PID" ]; then
  kill -9 $OLD_PID
  echo "   Killed PID $OLD_PID"
fi

# 4) 새 프로세스 기동
echo "→ Starting new jar"
nohup java -jar target/$JAR_NAME --spring.profiles.active=prod > app.log 2>&1 &

echo "[$(date)] Deploy script finished"
