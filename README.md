# 서비스 구성
## 1. 스펙 및 적용 기술
- java 17
- spring boot 3
- jpa
- mysql
- redis
- spring security
- oauth2 login (google)
- jwt
- github-actions

## 2.  기능 정리
### 1) 회원
- 회원 가입
- 점수 및 랭크 퍼센트 조회
- 회원 하트 차감 및 추가
### 2) 관리자
- 회원 목록 조회 및 회원 검색
- 퀴즈 목록 조회 및 퀴즈 검색
- 퀴즈 생성 수정 삭제
### 3) 퀴즈
- 문제 조회
- 문제 풀이
- 문제 풀이 시도 횟수 차감 및 초기화


# 💻 AWS 서버 구성

### 📦 VPC
* VPC 1개
* 인터넷 게이트웨이 연결 완료
### 🌍 가용영역 (AZ)
* 총 2개 사용 (예: ap-northeast-2a, ap-northeast-2c)
### 🧱 서브넷 구성
* AZ마다 2개씩 → 총 4개 서브넷
    * web-subnet-a / web-subnet-c → 프론트 EC2
    * was-subnet-a / was-subnet-c → 백엔드 EC2
### 🔁 라우팅 테이블
* web 서브넷들끼리 공유하는 라우팅 테이블
* was 서브넷들끼리 공유하는 라우팅 테이블
### 🖥️ EC2
* 프론트용 EC2 (web 서브넷)
* 백엔드용 EC2 (was 서브넷)
### 📡 Load Balancer
* 프론트 ALB (web용 EC2 대상 그룹)
* 백엔드 ALB (was용 EC2 대상 그룹)
### 🧭 Route53
* hyeonsu-side.com → 프론트 ALB 연결
* api.hyeonsu-side.com → 백엔드 ALB 연결
### 🛡️ 보안 그룹
* web SG (프론트 EC2 + ALB)
* was SG (백엔드 EC2 + ALB)
* mysql SG (RDS)
* redis SG (Elasticache)
### 🗃️ Database & Cache
* RDS (MySQL) → VPC 내부
* Elasticache (Redis) → VPC 내부


### 추후
* was 는 private 로 변경 
* NAT GATEWAY 로 설정
