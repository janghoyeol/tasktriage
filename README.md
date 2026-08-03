# TaskTriage

여러 채널(이메일, 메신저, 요청서 등)에서 들어오는 작업 요청을 한곳에 접수하면, AI가 긴급도·카테고리를 1차 판단해 우선순위를 정렬하고, 처리 상태와 SLA 마감을 추적해주는 개인용 작업 관리 도구입니다.

> 상세 기획은 [CLAUDE.md](./CLAUDE.md), 설계 결정 기록은 [docs/decisions](./docs/decisions)를 참고하세요.

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
Gate 2: FastAPI + Claude API → 카테고리/긴급도 + confidence score
   │
   ├─ confidence ≥ threshold → 자동 큐 배치
   └─ confidence < threshold → 사람 검토 대기 (NEEDS_REVIEW)
   ▼
Spring Boot: 상태 머신 + SLA 타이머
```

LLM은 무조건 호출하지 않고, 규칙 기반으로 걸러지지 않는 애매한 요청에만 씁니다. Gate 1 필터링 비율과 Gate 2 호출률/confidence 분포는 관리자 대시보드에서 지표로 확인할 수 있습니다. (진행 중)

## 기술 스택

Java 21 / Spring Boot 3 / Spring Security(JWT) / Spring Data JPA · PostgreSQL · Redis · Python(FastAPI) + Claude API · React + TypeScript · Docker Compose · GitHub Actions

## 구조

```
backend/           # Spring Boot API 서버
triage-service/    # FastAPI 분류 서비스
frontend/          # React 대시보드
docs/decisions/    # 설계 결정 기록 (ADR)
docs/api/          # OpenAPI 스펙
```

## 개발 중

이 프로젝트는 개발 진행 중입니다. 진행 상황은 [CLAUDE.md](./CLAUDE.md)의 로드맵을 참고하세요.
