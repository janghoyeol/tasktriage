# TaskTriage

여러 채널(이메일, 메신저, 요청서 등)에서 들어오는 작업 요청을 한곳에 접수하면, AI가 긴급도·카테고리를 1차 판단해 우선순위를 정렬하고, 처리 상태와 SLA 마감을 추적해주는 개인용 작업 관리 도구입니다.

> 상세 기획은 [CLAUDE.md](./CLAUDE.md), 설계 결정 기록은 [docs/decisions](./docs/decisions)를 참고하세요.

## 라이브 데모

| | URL |
|---|---|
| 프론트엔드 | https://tasktriage-frontend.onrender.com |
| 백엔드 API | https://tasktriage-backend.onrender.com |
| triage-service (FastAPI docs) | https://tasktriage-triage.onrender.com/docs |

backend/triage-service는 Render 무료 티어라 15분 무활동 후 슬립합니다. 오래 안 쓴 상태에서 처음 접속하면 **콜드스타트에 최대 2~3분** 걸릴 수 있습니다(실측치, [ADR 0009](./docs/decisions/0009-deployment-platform.md) 참고) — 라이브 데모 전이라면 링크를 미리 열어서 깨워두는 걸 권장합니다.

## 왜 만들었나

여러 곳에서 작업 요청이 쏟아질 때 무엇부터 처리해야 할지 판단하는 데 시간이 낭비된다는 문제에서 출발했습니다. "판단은 AI가 1차로 보조하되, 신뢰할 수 없는 판단은 반드시 사람이 검토한다"는 원칙으로 설계했습니다.

## 아키텍처: 2단계 게이트

```
작업 요청 등록
   │
   ▼
Gate 1: 규칙 기반 1차 분류 (키워드/정규식, LLM 호출 없음)
   │  애매한 경우만 통과
   ▼
Gate 2: FastAPI(triage-service) + Claude API → 카테고리/긴급도 + confidence score
   │
   ├─ confidence ≥ threshold → 자동 큐 배치 (QUEUED)
   └─ confidence < threshold → 사람 검토 대기 (NEEDS_REVIEW)
   ▼
Spring Boot(backend): 상태 머신 + SLA 타이머
```

LLM은 무조건 호출하지 않고, 규칙 기반으로 걸러지지 않는 애매한 요청에만 씁니다. 분류 모델도 가장 저렴한 `claude-haiku-4-5`를 씁니다 — Gate 1에서 걸러지지 않은, 복잡한 추론이 필요 없는 판단만 하기 때문입니다. Gate 1 필터링 비율, Gate 2 호출률/confidence 분포, 추정 비용 절감액은 관리자 대시보드(`/api/admin/metrics`)에서 지표로 확인할 수 있습니다.

## 기술 스택

Java 21 / Spring Boot 4.1 / Spring Security(JWT) / Spring Data JPA · PostgreSQL · Python(FastAPI) + Claude API · React 19 + TypeScript · Docker Compose · GitHub Actions · Prometheus + Grafana

> 브리핑 초안엔 Redis(중복 요청 방지/대기 큐)도 있었지만, 개인용 단일 사용자 도구로 스코프를 좁히면서 실제로는 만들지 않았습니다 ([ADR 0009](./docs/decisions/0009-deployment-platform.md) 참고).

## 구조

```
backend/           # Spring Boot API 서버
triage-service/    # FastAPI 분류 서비스 (Gate 2)
frontend/          # React 대시보드
prometheus/        # Prometheus 스크레이핑 설정
grafana/            # Grafana 데이터소스 프로비저닝
docs/decisions/     # 설계 결정 기록 (ADR)
docs/api/           # OpenAPI 스펙
```

## 로컬에서 실행하기

```bash
# 1. triage-service/.env.example을 triage-service/.env로 복사하고 실제 ANTHROPIC_API_KEY 채우기
cp triage-service/.env.example triage-service/.env

# 2. 전체 스택 (Postgres + backend + triage-service + frontend + Prometheus + Grafana) 기동
docker compose up -d --build
```

| 서비스 | 주소 |
|---|---|
| 프론트엔드 | http://localhost:5173 |
| 백엔드 API | http://localhost:8080 |
| triage-service (FastAPI docs) | http://localhost:8001/docs |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |

Grafana는 Prometheus 데이터소스가 자동으로 등록돼 있습니다. 대시보드는 grafana.com/dashboards에서 "Spring Boot" 또는 "JVM Micrometer"로 검색해 Import by ID로 가져오면 바로 backend 지표를 시각화할 수 있습니다.

## 모니터링

`backend`는 `/actuator/health`, `/actuator/info`, `/actuator/prometheus`를 노출합니다. Prometheus가 15초 간격으로 `/actuator/prometheus`를 스크레이핑하고, Grafana가 그걸 조회합니다. 자세한 구성은 [ADR 0010](./docs/decisions/0010-docker-compose-and-monitoring.md) 참고.

## 개발 중 AI(Claude Code) 협업 방식

이 프로젝트는 Claude Code를 코드 생성기가 아니라 **설계 논의 + 코드 리뷰 파트너**로 쓰면서 진행했습니다.

- **기능 구현 전 설계 논의**: Spring Security 필터 체인, JPA 연관관계, 상태 머신, CI/CD 같은 처음 다뤄보는 주제는 코드를 먼저 받지 않고 트레이드오프를 먼저 설명받은 뒤 합의하고 구현했습니다.
- **모든 주요 설계 결정을 ADR로 기록**: [docs/decisions](./docs/decisions)에 10개의 ADR이 있습니다. 각각 배경, 결정, 이유, 검토했던 대안, 그리고 실제로 겪은 삽질과 그 해결 과정까지 담겨 있습니다.
- **추측 대신 직접 확인하는 습관**: 라이브러리 버전이 바뀌면서(특히 Spring Boot 4로의 전환) 문서와 실제 동작이 다른 경우가 여러 번 있었습니다. 그때마다 Maven Central 메타데이터, 라이브러리 BOM, 심지어 `javap`로 프레임워크 클래스의 바이트코드/어노테이션까지 직접 열어봐서 확인한 뒤 대응했습니다.
- **원인을 100% 못 밝혀도 재현 가능하면 기록하고 넘어감**: 모든 버그를 끝까지 파고들기보다, 재현 가능한 해결책을 확인하면 원인 추정과 함께 문서로 남기고 실용적으로 넘어갔습니다.

가장 흥미로웠던 삽질 몇 가지:

| ADR | 내용 |
|---|---|
| [0005](./docs/decisions/0005-gate-pipeline-and-restclient-issue.md) | Spring `RestClient`와 uvicorn 간 HTTP 레벨 호환성 문제 — 원인은 100% 못 밝혔지만 재현 가능한 해결책으로 우회 |
| [0007](./docs/decisions/0007-api-integration-tests.md) | `javap`로 Boot 4의 `JacksonAutoConfiguration` 바이트코드를 직접 읽어서, Boot 4가 Jackson 3만 자동 등록한다는 걸 발견 |
| [0009](./docs/decisions/0009-deployment-platform.md) | Render.com 무료 Postgres가 30일 뒤 만료된다는 걸 재검증 과정에서 발견해 Neon으로 배포 전략을 바꿈 |
| [0010](./docs/decisions/0010-docker-compose-and-monitoring.md) | `/actuator/prometheus`가 403 — 0003에서 겪은 "인증은 통과해도 경로가 안 열려있으면 403" 패턴이 그대로 재현 |

## 진행 상황

1~4주차 로드맵은 [CLAUDE.md](./CLAUDE.md)를 참고하세요. 백엔드/AI 파이프라인/프론트엔드 핵심 기능, 테스트(유닛+통합 66개), CI, Docker Compose 기반 로컬 실행, 모니터링, 실제 배포(Render + Neon)까지 모두 완료했습니다.
