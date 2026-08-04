# 0005. Gate 1→Gate 2 파이프라인 연결

- 날짜: 2026-08-04
- 상태: 채택됨

## 배경

Task 등록 시 Gate 1(규칙 기반) → 애매하면 Gate 2(FastAPI, LLM) 순서로 분류하고, 결과에
따라 상태를 전이시키는 파이프라인을 연결했다.

## 결정

- **트랜잭션 경계**: `TriagePipeline.triage()`는 `Task` 엔티티가 아니라 `taskId`(Long)만
  받는다. Task 생성(`TaskService.createTask`)과 분류(`TriagePipeline.triage`)가 서로 다른
  트랜잭션이라, 앞 트랜잭션에서 반환된 엔티티는 뒤 트랜잭션 시점엔 detached 상태다.
  detached 엔티티를 그대로 넘겨 받아 필드를 바꾸면 변경사항이 DB에 반영되지 않는
  조용한 버그가 되므로, `taggedId`로만 받아 자기 트랜잭션 안에서 새로 조회하게 했다.
- **분류 실패는 작업 등록 자체를 막지 않는다**: Gate 2 호출이 실패해도(서비스 다운 등)
  `TaskController`에서 예외를 잡아 로그만 남기고, Task는 `SUBMITTED` 상태로 남는다.
  분류가 안 됐다고 사용자에게 500을 돌려주는 것보다, "일단 접수는 됐고 분류만 나중에
  다시 하면 된다"는 쪽이 더 견고하다고 판단했다.
- **Gate 1이 확정 못 지으면 Gate 2로**: category가 안 잡히면 무조건 애매한 것으로 보고
  Gate 2를 호출한다. urgency는 명시적 신호 없으면 MEDIUM으로 기본 처리한다
  (RuleBasedClassifier 구현 시점에 정한 결정).

## 삽질 기록: RestClient 기본 요청 팩토리와 uvicorn 간 호환성 문제

Spring의 `RestClient`(기본 요청 팩토리, 내부적으로 JDK `java.net.http.HttpClient` 사용)로
FastAPI(uvicorn) `/classify`에 POST하면 `Content-Length`는 정확히 계산되는데, 서버(uvicorn)가
바디를 아예 못 받는(`"input": null`) 문제가 있었다. 요청 인터셉터로 실제 전송되는 바이트를
찍어봐도 JSON은 멀쩡했다 — 즉 Java 쪽 직렬화 문제가 아니라 두 서버 간 HTTP/1.1 처리
방식 차이(추정: Expect: 100-continue 또는 커넥션 재사용 관련)로 보인다.

`RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory())`로 요청 팩토리를
`HttpURLConnection` 기반의 더 오래되고 단순한 구현으로 바꾸니 문제없이 동작했다. 원인을
100% 확정하진 못했지만, 재현되는 고정 값이라 이 설정으로 고정한다. 나중에 다른 서비스에서도
Java↔FastAPI 통신에 같은 증상이 나오면 여기부터 의심해볼 것.

## 검증

Gate 1 확정 케이스(명확한 버그+긴급 키워드 → RULE_BASED, 자동 QUEUED)와 Gate 2 케이스
(애매한 문장 → LLM 호출, confidence 0.65로 threshold 0.7 미만 → NEEDS_REVIEW)를 실제
Postgres + 실제 Claude API로 확인했다.
