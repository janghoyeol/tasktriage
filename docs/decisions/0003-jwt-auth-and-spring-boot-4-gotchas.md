# 0003. JWT 인증 구조 + Spring Boot 4 전환 관련 이슈

- 날짜: 2026-08-04
- 상태: 채택됨

## 배경

1주차 마지막 작업으로 stateless JWT 인증(회원가입/로그인/보호된 엔드포인트)을 구현했다.
브리핑 시점엔 "Spring Boot 3"이 최신이었지만 실제 셋업 시점엔 Spring Boot 4.1이 최신이라
그쪽으로 갔고(0002 이후 별도 확인), 그 과정에서 Boot 3 기준 자료와 다르게 동작하는
지점이 두 군데 있었다.

## 결정 / 구현

- **인증 방식**: JWT 기반 stateless 인증. `io.jsonwebtoken(jjwt)` 라이브러리로 토큰 발급/검증.
- **비밀번호**: BCrypt 해시.
- **DB 스키마 관리**: Flyway 마이그레이션 도입 (`db/migration/V1__create_users_table.sql`),
  `ddl-auto: validate`로 전환.
- **Refresh token은 이번 스코프에서 제외** — 액세스 토큰 만료(60분) 시 재로그인.

## 삽질 기록 (Spring Boot 4 관련, 다음에 또 겪지 않기 위해 기록)

### 1. `flyway-core`만 추가하면 자동 설정이 안 됨

Spring Boot 3까지는 `flyway-core`를 의존성에 추가하면 DataSource가 있을 때 자동으로
마이그레이션이 실행됐다. Boot 4부터는 자동 설정 클래스들이 거대한 `spring-boot-autoconfigure`
하나에서 기능별 전용 모듈(`spring-boot-hibernate`, `spring-boot-jdbc` 등)로 쪼개졌고,
Flyway도 예외가 아니어서 **`spring-boot-starter-flyway`를 명시적으로 추가해야** 자동 설정이
동작한다. `flyway-core`만 있으면 앱은 정상 기동되지만 마이그레이션은 조용히 실행되지 않고,
Hibernate의 스키마 검증(`ddl-auto: validate`)이 "테이블이 없다"며 기동 자체를 실패시킨다.

### 2. 커스텀 Security 필터에 `@Component`를 붙이면 안 됨

`JwtAuthenticationFilter`에 `@Component`를 붙였더니, 유효한 토큰으로도 보호된 엔드포인트가
항상 403이 나는 문제가 있었다. 원인 조사 중 실제로는 필터 자체는 정상 동작하고
(SecurityContext에 인증 정보가 올바르게 들어감을 로그로 확인) 인가 단계 직전까지도
인증 정보가 살아있었는데도 403이 나서 헤맸다 — 최종적으로 원인은 이게 아니라
**`/api/tasks`가 아직 매핑된 컨트롤러가 없는 경로였기 때문**으로 드러났다(2주차 작업 범위).
Spring이 인증은 통과시키되 핸들러를 못 찾는 경우 404가 아니라 403을 반환하는 것을 확인.

다만 조사 과정에서 `@Component` + `SecurityConfig`에서 직접 `addFilterBefore`로 등록하는
조합이 Spring Boot가 같은 필터를 서블릿 컨테이너의 일반 필터로 중복 등록할 수 있는
알려진 함정이라는 걸 확인했고, 실제 원인은 아니었지만 이미 알려진 안티패턴이라 그대로
고쳐서 유지하기로 했다 — `JwtAuthenticationFilter`는 `@Component`를 붙이지 않고
`SecurityConfig`에서 `new`로 직접 생성해 체인에 넣는다.

## 검증

`docs/api/openapi.yaml`에 정의된 `/api/auth/register`, `/api/auth/login`을 로컬 Postgres에
대해 curl로 직접 호출해 정상 동작(201/200), 중복 이메일(409 — 코드 상 처리돼 있으나 이번엔
직접 재확인은 못함), 잘못된 비밀번호(401), 유효성 검증 실패(400), 보호된 엔드포인트에
토큰 없이 접근(403), 유효한 토큰으로 접근(200 + 정확한 인증 정보)까지 확인했다.
자동화된 JUnit 테스트는 2주차(Task CRUD + 서비스 레이어 테스트) 때 함께 작성한다.
