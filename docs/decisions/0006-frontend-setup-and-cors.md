# 0006. 프론트엔드 셋업 + CORS

- 날짜: 2026-08-04
- 상태: 채택됨

## 결정

- **Vite + React + TypeScript**: Create React App은 더 이상 유지보수되지 않아 Vite로 시작.
- **라우팅**: `react-router-dom`. 인증 여부에 따라 `/login`, `/register`는 공개, 나머지는
  `ProtectedRoute`로 감싸서 미인증 시 `/login`으로 리다이렉트.
- **서버 상태 관리**: `@tanstack/react-query` 도입 예정(목록/상세 페이지부터 사용). 별도
  전역 상태 라이브러리(Redux 등)는 이 프로젝트 규모에서 과함 — 인증 상태만 React Context로,
  나머지 서버 데이터는 React Query 캐시로 충분하다고 판단.
- **인증 토큰 저장**: `localStorage`. 새로고침해도 로그인이 유지되어야 하는 개인용 도구
  성격에 맞음. XSS로 토큰이 탈취될 수 있다는 트레이드오프는 있지만, refresh token을
  아예 안 쓰는 현재 설계(액세스 토큰 60분 만료, 0002/0003 참고)에서는 피해 범위가
  제한적이라고 판단.

## 삽질 기록: CORS

프론트(`localhost:5173`)에서 백엔드(`localhost:8080`)로 로그인 요청을 보내니 preflight
(OPTIONS)는 200으로 통과하는데 실제 POST가 브라우저에서 `net::ERR_FAILED`로 막혔다.
Spring Security 쪽에 CORS 설정이 전혀 없었던 게 원인 — `SecurityConfig`에
`CorsConfigurationSource` 빈을 추가하고 필터체인에 `.cors(...)`를 연결해서 해결했다.
허용 origin은 `app.cors.allowed-origins` 설정값(콤마 구분)으로 뒀고, 4주차 배포 시 실제
프론트 URL을 추가하면 된다.

## npm audit 참고

`react-router-dom` 최신 버전에 `npm audit`이 "RSC Mode CSRF Bypass"(high)를 하나 띄운다.
설명을 보면 React Server Components 모드에 한정된 취약점인데, 이 프로젝트는 Vite 기반
순수 클라이언트 SPA라 RSC를 아예 안 쓴다. 구버전으로 내리면 오히려 SSR/RSC 관련
취약점이 더 많이 걸려서(구버전 쪽이 더 위험), 최신 버전을 그대로 유지하기로 했다.
