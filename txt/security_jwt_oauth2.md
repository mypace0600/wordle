# Security, JWT and OAuth2 Login

---

## 로그인 처리까지의 단계
1. 클라이언트 요청: localhost:8080으로 요청이 들어오면 Spring Security가 필터링.
2. 구글 로그인: 인증 정보가 없으면 구글 로그인 페이지로 리다이렉트.
3. 구글 인증 후 리다이렉트: 구글이 인증 코드를 반환하면 Spring Security가 이를 처리.
4. 이메일 추출: OAuth2AuthenticationToken에서 구글 사용자 이메일을 추출.
5. DB 처리: 이메일로 DB에서 유저 확인, 없으면 신규 사용자 생성.
6. JWT 토큰 발행: 인증 성공 후 JWT 토큰을 생성하고 클라이언트에 전달.
7. 리다이렉트: 클라이언트는 JWT 토큰을 받은 후 인증된 상태로 /home 화면 등으로 리다이렉트.



