#!/bin/bash
set -e  # 오류 발생 시 즉시 중단

APP_DIR="/home/${{ secrets.EC2_USER }}/app"
JAR_NAME="quiz-application.jar"  # QuizApplication에 맞춰 JAR 파일 이름 설정

echo "[$(date)] Deploy script started"

# 1) 작업 디렉터리로 이동
cd "$APP_DIR"

# 2) 최신 코드 가져오기 (이미 git pull이 GitHub Actions에서 이루어지므로 생략 가능)

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
