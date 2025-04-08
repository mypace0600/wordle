# 🔐 로그인/로그아웃 시나리오 정리

## 😀 로그인 시나리오

1. 사용자가 `http://localhost:5173` 에 접근하면
2. `main.jsx` 가 실행되며 `App.jsx` 를 호출합니다.
3. `App.jsx` 는 `/` 경로에 따라 `Index.jsx` 를 렌더링합니다.
4. `Index.jsx` 는 백엔드에 로그인 여부 확인을 위해 `http://localhost:8080/api/auth/check` API를 호출합니다.
5. 이때 백엔드는 Spring Security 설정(`SecurityConfig`)에 따라 다음 순서대로 요청을 처리합니다:
   - **CORS 필터**: 클라이언트에서 오는 요청을 허용합니다.
   - **authorizeHttpRequests**: `/api/auth/**` 경로는 `permitAll()` 설정으로 인증 없이 접근 가능.
   - **DispatcherServlet**을 통해 `AuthController`의 `checkAuth()` 메서드로 요청이 전달됩니다.
   - **jwtAuthenticationFilter**가 동작하여 쿠키에 담긴 AccessToken을 검사하고,  
     AccessToken이 존재할 경우 이를 통해 인증된 사용자로 등록합니다.  
     (→ **AccessToken 기반 인증 처리** 수행)
   
6. 최초 접근 시에는 쿠키에 토큰이 없어 `401 Unauthorized` 응답이 반환됩니다.
7. 클라이언트는 로그인되지 않은 상태로 판단하고 `/splash` 페이지로 라우팅합니다.
8. `Splash` 페이지에서 사용자는 Google 로그인 버튼을 클릭하게 되며,  
   Google OAuth2 로그인 화면으로 리디렉션됩니다.
9. 사용자가 로그인 후 리디렉션되면 Spring Security의 `oauth2Login()` 필터가 동작합니다.
   - `customOAuth2UserService`가 동작하여 Google에서 전달받은 정보를 바탕으로  
     **이메일의 정보로 `User` 엔티티를 생성 및 저장**합니다.
10. 로그인 성공 시, `oAuth2AuthenticationSuccessHandler`가 동작하여 AccessToken을 발급하고 쿠키에 저장한 뒤  
    `http://localhost:5173`으로 리다이렉트됩니다.
11. 클라이언트는 다시 `App.jsx` → `Index.jsx` 순으로 실행되며,  
    이번에는 AccessToken이 존재하므로 `jwtAuthenticationFilter`가 인증을 수행하고,  
    백엔드에서 정상 응답을 받으면 `/home` 페이지로 전환됩니다.

---

## 👋 로그아웃 시나리오

1. 사용자가 로그아웃 버튼을 클릭하면  
   `LogOutButton.jsx`에서 `http://localhost:8080/api/auth/custom-logout`을 호출합니다.
2. 백엔드에서는 다음 순서로 로그아웃 요청을 처리합니다:

   - `SecurityConfig` 필터 체인 통과
   - `jwtAuthenticationFilter` 통과
   - `DispatcherServlet`을 통해 `AuthController`의 `logout()` 메서드 실행
   - 해당 메서드에서 **세션 삭제 + 쿠키 삭제** 처리가 이루어집니다.

3. 클라이언트는 응답을 받은 뒤 쿠키를 클라이언트 측에서도 삭제하고 `/splash` 페이지로 이동합니다.

----
## 🛠️ 차후 계획
1. Refresh Token 도입  
   → 토큰 만료 시 자동 갱신을 통해 UX 개선
2. 화면 잠금 및 간편 비밀번호 기능 추가  
   → 장시간 자리 비움 시 보안 강화
