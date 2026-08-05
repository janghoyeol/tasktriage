# 0009. 배포 플랫폼 최종 결정

- 날짜: 2026-08-04
- 상태: 채택됨 (실제 배포 작업은 4주차에 진행)

## 배경

1일차에 예산 제약("웬만하면 돈 안 내고 싶어") 때문에 Render.com을 잠정 후보로
잡아뒀었다. 3주차 마무리 시점에 실제로 확정하기 전에, 요금제가 자주 바뀌는
영역이라 오늘(2026-08-04) 기준으로 다시 확인했다.

## 검토한 후보

- **AWS EC2 + RDS**: 2025-07-15 이후 생성된 계정은 12개월 무료 티어 대신
  6개월 만료 $200 크레딧으로 바뀜. 설정도 이 프로젝트 규모에 비해 무겁고
  (VPC, 보안그룹, RDS 인스턴스 관리 등), 신입 개발자 포트폴리오 기간(1개월) +
  이후 면접 기간 동안 계속 떠 있어야 하는데 크레딧 소진/만료 리스크가 크다.
  **제외.**
- **Fly.io**: 2024년에 실질적인 무료 티어가 사라졌고 지금은 7일 2시간
  체험판 이후 월 최소 ~$2. 브리핑 원안에 있던 선택지지만 "무료"라는 전제가
  더 이상 성립하지 않아 **제외.**
- **Render.com**: 카드 등록 없이 웹 서비스(750시간/월, 15분 무활동 시
  슬립), 정적 사이트, Postgres, Redis까지 전부 무료 티어 제공. 다만 **무료
  Postgres는 생성 후 30일 뒤 만료, 14일 유예 후 삭제**된다는 게 확인됨 —
  포트폴리오/면접용으로 몇 달간 계속 살아있어야 하는 이 프로젝트 성격과
  안 맞는다.
- **Neon**: Postgres 전용 매니지드 DB. 무료 티어가 시간제한 없이 영구
  (0.5GB 스토리지, 프로젝트당 월 100 컴퓨트시간, 카드 불필요) — Render
  Postgres의 30일 만료 문제를 정확히 해결한다.

## 결정: Render(앱 3종) + Neon(DB) 하이브리드

| 컴포넌트 | 플랫폼 | 비고 |
|---|---|---|
| backend (Spring Boot) | Render Web Service (무료) | 15분 무활동 후 슬립, 재요청 시 콜드스타트 70~140초(실측, 0.1 CPU) |
| triage-service (FastAPI) | Render Web Service (무료) | 위와 동일한 슬립 정책, 콜드스타트는 backend보다 짧음 |
| frontend (React 정적 빌드) | Render Static Site (무료) | 정적 사이트는 슬립 없음 — 항상 빠름 |
| DB | **Neon Postgres (무료)** | Render 대신 선택 — 만료 없음 |

전부 $0/월. 두 웹 서비스가 각자 슬립하기 때문에 750시간/월 한도는 저트래픽
포트폴리오 데모 용도로는 넉넉하다.

### 알려진 트레이드오프 (면접 데모 시 참고)

- **콜드스타트 (실측)**: 처음엔 "~1분"으로 추정했는데, 실제 배포해보니 backend
  (Spring Boot/JVM)는 **첫 기동에 140초, 재시작 시에도 70초** 넘게 걸렸다.
  원인은 Render 무료 티어의 CPU가 **0.1코어**뿐이라 — 로컬에서 5~10초면
  끝나는 Hibernate/Flyway/Spring context 초기화가 여기선 10배 이상 느리다.
  triage-service(FastAPI, 훨씬 가벼운 런타임)는 이 정도로 느리진 않았다.
  면접 중 라이브 데모라면 **backend 링크를 최소 2~3분 전에는 미리 열어서
  깨워둬야** 한다 — README에도 이 실측치로 적어뒀다.
- Neon 무료 티어 컴퓨트시간(월 100시간)을 넘기면 그 달은 접속이 제한될 수
  있음. 이 프로젝트 트래픽 규모면 초과 가능성은 낮다.
- **Redis는 배포 대상에서 제외**: 브리핑 원안 기술 스택엔 있었지만 실제
  구현 단계에서 "개인용 도구, 단일 사용자"로 스코프가 좁혀지면서 중복 요청
  방지/대기 큐 기능 자체를 만들지 않았다. 없는 걸 배포할 필요는 없어서
  Render 무료 Redis도 쓰지 않는다.

## 진행 상황 (2026-08-05 갱신)

- backend/triage-service Dockerfile 작성, docker-compose 통합 완료 (ADR 0010)
- Neon 프로젝트 생성 (Postgres 18, Singapore 리전, Neon Auth 비활성화 — 자체
  JWT 인증과 중복이라 불필요)
- Render에 triage-service, backend 배포 완료. Neon 연결은 `DB_HOST/PORT/NAME/
  USERNAME/PASSWORD` + `DB_SSLMODE=require` + `DB_CHANNEL_BINDING=require`
  환경변수로 구성 (pgjdbc 파라미터명이 Neon 연결 문자열의 `channel_binding`과
  달리 camelCase `channelBinding`이라 별도 확인 필요했음)
- 남은 것: frontend Render Static Site 배포, backend `CORS_ALLOWED_ORIGINS`를
  실제 프론트 URL로 갱신, 배포된 URL에서 전체 플로우 재검증
