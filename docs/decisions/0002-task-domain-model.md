# 0002. Task 도메인 모델 확정

- 날짜: 2026-08-03
- 상태: 채택됨

## 배경

브리핑 초안의 ERD에 세 가지 애매한 지점이 있었다: (1) Task와 User의 관계가 요청자/담당자 중 무엇을 가리키는지, (2) category를 enum으로 고정할지 자유 문자열로 둘지, (3) confidence threshold를 어디에 저장할지.

## 결정

1. **Task.owner_id 단일 FK 유지.** requester/assignee를 분리하지 않는다.
2. **Task.category는 고정 enum.** 값: `BUG, FEATURE_REQUEST, SUPPORT, BILLING, OTHER`.
3. **confidence threshold는 application.yml 설정값으로 시작.** DB 테이블화는 나중에 필요해지면 별도 ADR로 결정한다.

## 이유

1. 개인/소규모 팀을 위한 포트폴리오 프로젝트 규모에서는 한 사람이 요청도 하고 처리도 하는 경우가 대부분이라, 요청자/담당자 분리는 지금 단계에서 과설계다. 나중에 필요해지면 `assignee_id` 컬럼을 추가하는 것으로 충분히 확장 가능하다 (마이그레이션 1개로 해결).
2. Gate 1(규칙 기반 분류)이 정규식/키워드로 카테고리를 판별하려면 결국 카테고리 집합이 유한해야 한다. enum으로 고정하면 DB 제약(CHECK 또는 Postgres enum 타입)과 Gate 1 로직, 프론트 필터 UI가 모두 일관되게 맞아떨어진다.
3. 1주차~2주차에는 threshold를 실험적으로 여러 번 바꿔볼 가능성이 높은데, 이 단계에서는 설정 파일 수정 + 재배포가 DB 테이블 관리보다 빠르다. 4주차 관리자 대시보드 작업 시 필요하면 DB로 옮긴다.

## 대안

- requester_id/assignee_id 분리 — 실무에는 더 가깝지만 지금 범위에서는 오버엔지니어링으로 판단해 보류.
- category 자유 문자열 — 유연하지만 Gate 1 규칙과 대시보드 집계가 불안정해져서 기각.
- threshold DB 테이블 — 관리자 조정 UX는 좋지만 지금 단계에서 과함. 4주차 재검토 예정.
