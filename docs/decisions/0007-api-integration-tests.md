# 0007. API 통합 테스트 (Testcontainers + MockMvc)

- 날짜: 2026-08-04
- 상태: 채택됨

## 배경

3주차 작업으로 서비스 레이어 유닛 테스트(Mockito, 2주차)에 이어 실제 HTTP 요청 -
Security 필터 체인 - JSON 직렬화 - 실제 DB까지 전부 타는 API 통합 테스트를 추가했다.
회원가입 -> 로그인 -> 인증 -> Task 등록 -> 조회 전체 흐름, 중복 이메일 409, 잘못된
비밀번호 401을 검증한다.

## 결정 / 구현

- **DB**: `AbstractIntegrationTest`에서 Testcontainers로 진짜 Postgres 컨테이너를
  띄운다 (`@ServiceConnection`으로 별도 설정 없이 DataSource 자동 연결). 로컬
  docker-compose에 의존하지 않고 CI에서도 Docker만 있으면 그대로 동작한다.
- **HTTP 계층**: `MockMvc` (`@AutoConfigureMockMvc`) 사용. 실제 포트를 열지 않고도
  서블릿 필터 체인(Spring Security 포함)을 그대로 통과시켜 검증할 수 있고, 아래
  이유로 `TestRestTemplate`보다 안정적이었다.
- **JSON 처리**: 요청 바디는 text block으로 직접 작성, 응답 파싱은 `JsonPath`
  (`spring-boot-starter-test`에 포함된 `com.jayway.jsonpath`)로 필드만 추출.
  `ObjectMapper`로 DTO를 직렬화/역직렬화하지 않는다 (아래 삽질 참고).
- Gate 1(규칙 기반)에서 확정 분류되는 문구("Something is broken and urgent")를
  써서, 외부 Gate 2(FastAPI) 서비스가 테스트 실행 환경(CI 포함)에 없어도 안정적으로
  통과하게 했다.

## 삽질 기록

### 1. `TestRestTemplate` 사용 시 `@ConditionalOnMissingBean` 예외로 컨텍스트 로딩 자체가 실패

Boot 4에서 `TestRestTemplate`은 `spring-boot-starter-test` 밖으로 분리되어
`spring-boot-resttestclient` 모듈(+`@AutoConfigureTestRestTemplate`)이 필요하다는
것까지는 확인하고 추가했는데, 그 다음
`TestRestTemplateTestAutoConfiguration.testRestTemplate` 빈을 처리하는 과정에서
`@ConditionalOnMissingBean did not specify a bean using type, name or annotation
and the attempt to deduce the bean's type failed`라는, 우리 코드가 아니라 Boot
프레임워크 내부에서 나는 예외로 컨텍스트 로딩 자체가 실패했다. `BackendApplicationTests`
(TestRestTemplate 미사용)는 정상 통과해서 Testcontainers/Postgres 연결 자체는
문제가 없다는 걸 확인했지만, 이 자동설정 내부 문제는 원인을 더 파기보다 — 최신
(4.1.0, 매우 최근 릴리스) Boot 버전에서 발생한 이슈로 보고 — 더 표준적이고 실무에서도
널리 쓰이는 `MockMvc` 방식으로 전환해 우회했다. `spring-boot-starter-webmvc-test`에
이미 포함되어 있어 추가 의존성도 필요 없었다.

### 2. Boot 4의 `JacksonAutoConfiguration`은 Jackson 3만 자동 등록한다

`MockMvc`로 전환한 뒤에도 테스트에서 `@Autowired ObjectMapper`가
`NoSuchBeanDefinitionException`으로 실패했다. `spring-boot-jackson` 모듈의
`JacksonAutoConfiguration` 클래스를 직접 열어보니(`javap`) 클래스 조건이
`@ConditionalOnClass(tools.jackson.databind.json.JsonMapper.class)` —
즉 Boot 4가 새로 지원하는 **Jackson 3**(`tools.jackson.*` 네임스페이스)만 보고
있었다. 이 프로젝트는 `jjwt-jackson`이 런타임에 끌어오는 **Jackson 2**
(`com.fasterxml.jackson.databind`, jjwt 토큰 직렬화용)만 갖고 있어서 조건이
안 맞았던 것.

한편 `spring-boot-starter-webmvc`는 컴파일 스코프로 Jackson 3
(`tools.jackson.core:jackson-databind:3.1.4`)을 끌어오기 때문에, 실제 REST
컨트롤러의 JSON 응답 자체는 (Boot 4가 새로 지원하는 Jackson 3 기반 HTTP 메시지
컨버터를 통해) 문제없이 동작한다 — 브라우저로 확인한 프론트-백엔드 연동이 이미
정상이었던 이유. 테스트에서만 Jackson 2 `ObjectMapper` 빈이 없어서 걸린 것.

**대응**: 테스트에서 Jackson 2/3 어느 쪽 `ObjectMapper`/`JsonMapper`도 직접
autowire하지 않기로 했다. 요청 바디는 text block 문자열로 직접 작성하고, 응답에서
필요한 필드(`accessToken`, `id`)만 `JsonPath.read(...)`로 뽑아 쓴다. 의존성 추가나
Jackson 버전 통일 없이 가장 단순하게 우회하는 방법이었다.

## 검증

`./mvnw test` 전체 스위트 66개 테스트(유닛 61 + 통합 3, `BackendApplicationTests`
1개 포함) 전부 통과. Testcontainers가 매 실행마다 Postgres 컨테이너를 띄우고
정리하는 것도 확인했다.
