# TaskTriage — 프로젝트 브리핑

> 이 문서는 claude.ai에서 기획을 마친 내용을 정리한 프로젝트 브리핑입니다.
> Claude Code는 이 문서를 참고하여 설계·구현을 진행합니다.

## 0. 개발자 배경 (톤 설정용)

- 백엔드 개발 실무 경험이 없는 주니어/완전 초보 개발자
- Java, React, Python은 프로젝트 단위로 다뤄본 적은 있으나, 실무형(테스트, CI/CD, 배포, 인증)은 처음
- 목표: 이력서/면접용 포트폴리오 프로젝트를 설계~배포까지 직접 완주
- **코드를 대신 다 짜주기보다, 단계별로 설명하며 진행할 것** (특히 Spring Security, JPA 연관관계, 상태 머신, CI/CD 부분은 처음 접함)
- 목표 기간: 약 1개월 (시작일 2026-08-03)
- 취업 목표 우선순위: 1순위 Claude Code 사용 경험을 우대하는 회사, 2순위 백엔드 개발자 채용 공고

## 1. 프로젝트 개요

**이름(가칭)**: TaskTriage
**한 줄 정의**: 프리랜서/1인 개발자/소규모 팀이 여러 곳에서 들어오는 작업 요청을 한곳에 접수하면, AI가 긴급도·카테고리를 판단해 우선순위를 자동 정렬해주고, 처리 상태와 마감(SLA)을 추적해주는 개인용 작업 관리 도구.

**개발 동기(면접용 스토리)**: 여러 채널(이메일, 메신저, 요청서 등)에서 작업 요청이 쏟아질 때 무엇부터 처리해야 할지 판단하는 데 시간이 낭비된다는 문제의식에서 출발. "판단은 AI가 1차로 보조하되, 신뢰할 수 없는 판단은 반드시 사람이 검토한다"는 원칙으로 설계.

## 2. AI 활용 철학 (이 프로젝트의 차별점)

기존 포트폴리오 프로젝트(Curiosity Teacher)에서 썼던 "LLM 호출 비용·품질 게이팅" 사고를 백엔드 아키텍처의 한 컴포넌트로 재사용:

```
[작업 요청 등록]
     │
     ▼
[Gate 1: 규칙 기반 1차 분류] (키워드/정규식 — LLM 호출 없음)
     │  분류 애매한 경우만 통과
     ▼
[Gate 2: Python(FastAPI) 서비스 — Claude API로 카테고리/긴급도 분류 + confidence score]
     │
     ├─ confidence ≥ threshold → 자동으로 우선순위 큐에 배치
     └─ confidence < threshold → "검토 필요" 상태로 사람 확인 대기
     ▼
[Java/Spring Boot: 상태 머신 기반 작업 처리 + 마감(SLA) 타이머]
```

목적: "AI를 무조건 쓰는 게 아니라, 비용·신뢰성 관점에서 언제/어디에 쓸지 설계한 근거"를 보여주는 것. Gate 1에서 걸러지는 비율, Gate 2 호출률/confidence 분포를 관리자 대시보드에서 지표로 보여줄 것.

개발 과정에서 Claude Code를 이렇게 활용:

- 기능 구현 전 설계안을 먼저 논의하고 트레이드오프를 리뷰받기
- 테스트 케이스를 먼저 설계한 뒤 구현 (TDD식 협업)
- PR 단위로 코드 리뷰 요청 → README에 "AI를 코드 생성기가 아니라 설계/리뷰 파트너로 사용했다"는 내용을 개발 로그 형태로 기록
- 주요 결정 사항은 `docs/decisions/`에 짧은 ADR로 기록

## 3. 기술 스택

| 영역 | 기술 |
|---|---|
| 백엔드 | Java 21, Spring Boot 3, Spring Security(JWT), Spring Data JPA |
| DB | PostgreSQL |
| 캐시/큐 | Redis (중복 요청 방지, 대기 큐 관리) |
| AI 분류 서비스 | Python (FastAPI) + Claude API |
| 프론트엔드 | React, TypeScript |
| 배포 | Docker Compose → GitHub Actions(CI/CD) → AWS EC2 + RDS (또는 Fly.io, 3주차에 결정) |
| 모니터링 | Spring Actuator + Prometheus + Grafana (최소 구성) |
| 문서화 | OpenAPI(Swagger) |

## 4. 도메인 모델 (ERD 초안)

```
User
 - id, name, email, password_hash, role(OWNER/ASSIGNEE)
   ※ 개인용 도구라 역할은 단순하게: 요청 등록자 vs 처리 담당자

Task (작업 요청)
 - id, title, description
 - category (nullable, AI가 채움)
 - urgency (LOW/MEDIUM/HIGH/URGENT)
 - status (SUBMITTED → TRIAGED → QUEUED → IN_PROGRESS → DONE → ARCHIVED)
   ※ confidence 낮으면 NEEDS_REVIEW 상태로 분기
 - source (EMAIL/MESSAGE/MANUAL 등 — 어디서 들어온 요청인지)
 - due_at (SLA 마감)
 - owner_id (FK User)
 - created_at, updated_at

TriageLog (분류 기록)
 - id, task_id (FK)
 - gate (RULE_BASED / LLM)
 - llm_called (boolean) ← 비용 절감률 지표로 활용
 - confidence_score
 - result_category, result_urgency

TaskStatusHistory (상태 이력)
 - id, task_id (FK), from_status, to_status, changed_at

SLARule (마감 정책)
 - id, urgency, response_time_hours
```

## 5. API 명세 초안

```
POST   /api/auth/register        회원가입
POST   /api/auth/login           로그인, JWT 발급
POST   /api/tasks                작업 요청 등록 (Gate 1→2 트리거)
GET    /api/tasks                작업 목록 (필터: status, urgency)
GET    /api/tasks/{id}           작업 상세 + 상태 이력
PATCH  /api/tasks/{id}/status    상태 전이
GET    /api/tasks/{id}/triage-log 분류 근거 조회
GET    /api/dashboard/summary    SLA 임박/위반, 상태별 개수
GET    /api/admin/metrics        Gate별 LLM 호출률, 비용 절감 추정치
```

## 6. 4주 로드맵

- **1주차 — 설계 & 셋업**: ERD 확정, OpenAPI 명세 작성, Spring Boot 프로젝트 셋업, JWT 인증 기본 구조, Python(FastAPI) 분류 서비스 스켈레톤 + Claude API 연동 테스트
- **2주차 — 핵심 백엔드**: Task CRUD + 상태 머신, Gate 1→Gate 2 파이프라인 연결, SLA 타이머, 서비스 레이어 유닛 테스트(JUnit) 작성
- **3주차 — 프론트 + 통합/CI**: React 대시보드(작업 목록/상세/SLA 현황), API 통합 테스트, GitHub Actions CI 파이프라인, 배포 플랫폼 최종 결정
- **4주차 — 배포 & 마무리**: Docker Compose 배포, 모니터링 연결, README/개발 로그 정리 (Gate 구조, 비용 절감률, 설계 결정 기록 포함), 버퍼

## 7. 완료 기준 (Definition of Done)

- [ ] REST API 설계 문서(OpenAPI) 존재
- [ ] JWT 인증/인가 동작
- [ ] 서비스 레이어 유닛 테스트 + API 통합 테스트 존재
- [ ] GitHub Actions로 빌드+테스트 자동화
- [ ] 실제 배포된 URL에서 회원가입~작업 등록~AI 분류~상태 변경까지 동작
- [ ] 기본 모니터링(헬스체크, 간단 지표) 확인 가능
- [ ] README에 아키텍처 다이어그램, Gate 구조 설명, 개발 중 AI 협업 방식 기록

## 8. 레포 구조

모노레포로 구성:

```
claude_code/
├── backend/           # Spring Boot (Java 21)
├── triage-service/    # FastAPI (Python) + Claude API
├── frontend/          # React + TypeScript
├── docs/
│   ├── decisions/     # ADR (Architecture Decision Record)
│   └── api/           # OpenAPI 스펙
└── docker-compose.yml # (4주차에 추가)
```

## 9. 협업 방식 (Claude Code 참고용)

- 코드를 한 번에 다 짜서 넘기지 말 것. 특히 Spring Security, JPA 연관관계, 상태 머신, CI/CD는 설계 의도와 트레이드오프를 먼저 설명하고 합의한 뒤 구현할 것.
- 주요 설계 결정은 `docs/decisions/NNNN-title.md` 형식의 짧은 ADR로 남길 것.
- 새 기능은 가능하면 테스트를 먼저 설계하고 나서 구현(TDD식)할 것.
