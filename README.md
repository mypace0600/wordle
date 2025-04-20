## ✅ 완료 작업 목록

- ⏱️ 시간 경과에 따른 하트 자동 회복
- 🧠 문제 풀이 성공 시 점수 부여 점검
- 📊 사용자 점수 조회 및 퍼센트 계산 점검
- 📝 Logback 설정을 통한 로그 파일 저장 및 30일 경과 시 압축 정리
- 💡 퀴즈 풀이 시 힌트 모달 생성
- 🛠️ 관리자 페이지에 유저 파트 신설 및 유저 검색 기능 구현
- 🏠 AWS 서버 세팅작업
- 🔁 CI/CD 적용 (GitHub Actions 등)
---

## 🛠️ 남은 작업 목록

### 🚀 AWS 배포

- ec2 에 redis 연결
- github secret 에 .env 파일로 정리한 환경변수들 추가
- spring boot 에 github-action 관련 설정
- 두 개의 서브넷에서도 github-action 동작하도록 설정
- 배포 후 테스트
- 고정 IP 설정
- 도메인 구입 후 적용
- SSL 적용

---

## 🧩 추후 작업 목록
- 🐳 Docker 파일 적용
- 🎬 광고 SDK 적용 후 보상형 광고 시청 시 하트 지급 기능 구현




---

# EC2 Java 개발 환경 설정

EC2 인스턴스에 Java 17 개발 환경을 설정하는 과정입니다.

---

## 📦 필수 패키지 설치

```bash
# Git 설치 (이미 설치되어 있는 경우 생략 가능)
sudo apt-get update
sudo apt-get install git -y
```

## ☕ OpenJDK 17 설치

```bash
# OpenJDK 17 설치
sudo apt install openjdk-17-jdk -y
```

### ✅ Java 설치 확인

```bash
java -version
```

예시 출력:
```
openjdk version "17.0.10" 2024-01-16
OpenJDK Runtime Environment (build 17.0.10+7-Ubuntu-122.04.1)
OpenJDK 64-Bit Server VM (build 17.0.10+7-Ubuntu-122.04.1, mixed mode, sharing)
```

---

## 🔧 JAVA_HOME 환경 변수 설정

```bash
# 환경 변수 설정 파일 열기
sudo vi /etc/environment
```

내용에 아래 항목 추가 (편집 모드에서 `i` 누르고 아래 내용 삽입):

```bash
JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
```

편집 완료 후:
- `Esc` 키 → `:wq` → Enter 로 저장 후 종료

### 📌 변경 사항 적용

```bash
source /etc/environment
echo $JAVA_HOME
```

정상적으로 설정되었다면 아래처럼 출력됩니다:

```
/usr/lib/jvm/java-17-openjdk-amd64
```

---
