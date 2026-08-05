# 0010. Docker Compose 통합 + Prometheus/Grafana 모니터링

- 날짜: 2026-08-05
- 상태: 채택됨

## 배경

4주차 작업으로, `docker-compose up` 한 번으로 backend/triage-service/frontend/DB/모니터링까지
전체 스택이 로컬에서 뜨도록 만들었다. 브리핑 원안엔 Prometheus+Grafana가 "최소 구성"으로
적혀 있었는데, 실제로 어디까지 할지(Actuator 지표만 vs 시각화 대시보드까지)는 진행하면서
정해야 했다.

## 결정

### 3개 앱 서비스 모두 Dockerfile 작성 (멀티스테이지 빌드)

- **backend**: `eclipse-temurin:21-jdk`로 빌드(`./mvnw package`) → `eclipse-temurin:21-jre`로
  실행. JDK 대신 JRE만 최종 이미지에 넣어서 크기를 줄였다.
- **triage-service**: `python:3.12-slim` 위에 `pip install`만 하는 단일 스테이지 —
  컴파일 단계가 없는 언어라 멀티스테이지가 필요 없다.
- **frontend**: `node:20-slim`으로 `npm run build` → 결과물(`dist/`)만 `nginx:alpine`에 복사.
  React Router의 클라이언트 사이드 라우팅 때문에 `nginx.conf`에 `try_files $uri /index.html`
  폴백을 넣었다 — 안 넣으면 `/tasks/5`를 새로고침할 때 nginx가 404를 낸다.
  또한 `VITE_API_BASE_URL`은 Vite가 **빌드 시점**에 번들에 박아 넣는 값이라, 컨테이너
  `environment:`가 아니라 `docker build --build-arg`(compose의 `build.args`)로 넘겨야 한다 —
  런타임 환경변수로 착각하기 쉬운 지점이라 기록해둔다.

### Prometheus + Grafana까지 (최소 구성이 아니라 시각화까지)

Actuator `health`/`info`만으로도 DoD의 "기본 모니터링"은 충족하지만, 실제로 시계열
지표를 모으고 시각화하는 것까지 해보기로 했다. `micrometer-registry-prometheus`
의존성을 추가하면 `/actuator/prometheus`에 Prometheus가 스크레이핑할 수 있는 형식으로
지표가 노출된다. `docker-compose.yml`에 `prometheus`, `grafana` 서비스를 추가하고,
Grafana는 `grafana/provisioning/datasources/`에 Prometheus 데이터소스를 미리 등록해둬서
컨테이너가 뜨자마자(수동 설정 없이) 바로 조회 가능하게 했다.

**triage-service(FastAPI)는 계측하지 않음** — Micrometer 같은 지표 라이브러리가 없어서
`prometheus-fastapi-instrumentator` 같은 새 의존성이 필요한데, 지금 스코프에서는
backend 지표만으로 "AI 파이프라인 비용/신뢰성"이라는 이 프로젝트의 핵심 지표
(Gate 1/2 호출률, confidence 분포 — `/api/admin/metrics`에 이미 있음)를 보여주는 데
충분하다고 판단해 보류했다.

**Grafana 대시보드는 직접 만들지 않음** — 패널을 하나하나 손으로 짜는 대신, Grafana의
"Import by ID" 기능으로 공개된 커뮤니티 Spring Boot/Micrometer 대시보드를 가져와 쓰는
쪽을 권장한다(grafana.com/dashboards에서 "Spring Boot" 또는 "JVM Micrometer"로 검색).
검증 안 된 대시보드 JSON을 처음부터 작성하는 것보다, 이미 많이 쓰이고 검증된 걸
가져오는 게 실용적이라고 판단했다.

## 삽질 기록: `/actuator/prometheus`가 403

Prometheus 컨테이너가 backend를 스크레이핑하는데 `health: down`, `lastError: server
returned HTTP status 403`이 떴다. 원인은 0003에서 이미 겪었던 것과 같은 패턴 —
`SecurityConfig`의 `permitAll` 목록에 `/actuator/health`만 있고 `/actuator/prometheus`는
없어서 Spring Security가 막은 것. `application.yml`에서 노출하기로 한 세 엔드포인트
(`health`, `info`, `prometheus`)만 정확히 `permitAll`에 추가해서 해결 — 그 이상 넓게
열지 않았다.

## 검증

`docker-compose up -d --build`로 6개 컨테이너(db/backend/triage-service/frontend/
prometheus/grafana) 전부 기동 확인. 브라우저로 프론트(`localhost:5173`)에 접속해
회원가입→로그인→Task 등록까지 직접 수행했고, 새로 등록한 Task가 실제로 컨테이너
네트워크를 통해 `triage-service`(Claude API 호출)까지 갔다 와서 `BUG/MEDIUM/QUEUED`로
분류되는 것까지 확인했다. Prometheus 타겟 상태(`health: up`)와 Grafana 데이터소스
헬스체크(`Successfully queried the Prometheus API`)도 API로 직접 확인했다. 백엔드
테스트 66개는 이번 변경(SecurityConfig, pom.xml) 이후에도 전부 통과.
