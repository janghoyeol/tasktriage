# 0004. Gate 2 분류 서비스 (FastAPI + Claude API) 스켈레톤

- 날짜: 2026-08-04
- 상태: 채택됨

## 배경

1주차 마지막 작업으로 Gate 2(LLM 기반 분류) 역할을 하는 FastAPI 서비스의 뼈대를 만들고
실제 Claude API 호출까지 검증했다.

## 결정

- **모델**: `claude-haiku-4-5`. 분류 작업(카테고리/긴급도 판단)은 복잡한 추론이 필요 없고,
  Gate 1에서 걸러지지 않은 애매한 요청만 도달하므로 가장 저렴한 모델로 충분하다고 판단
  (호출당 대략 $0.0005 수준 — 개발/테스트 전체 합쳐도 몇 달러 안쪽).
- **구조화 출력(Structured Outputs) 사용**: `output_config.format`으로 JSON 스키마를 강제해서
  Claude 응답이 항상 `{category, urgency, confidence, reasoning}` 형태를 따르도록 함.
  일반 프롬프팅으로 "JSON으로만 답해줘" 하는 방식보다 파싱 실패 가능성이 없어 재시도/예외
  처리 로직이 필요 없어진다.
- **Gate 1과의 결합 안 함**: 이 서비스는 분류만 담당. 규칙 기반 1차 필터링(Gate 1)과
  confidence threshold에 따른 라우팅(자동 큐 배치 vs NEEDS_REVIEW)은 2주차에 백엔드에서
  파이프라인으로 연결한다.
- **Python 3.9 → 3.12 업그레이드**: 최신 FastAPI/uvicorn/pydantic-settings가 Python 3.10+을
  요구해서 실제로 설치가 막혔다. 1일차에 "문제되면 그때 올리자"고 미뤄뒀던 게 실제로
  문제가 됐고, 마침 3.9는 이미 지원 종료(EOL)된 버전이라 3.12로 올렸다.

## 검증

로컬에서 서비스를 띄우고 실제 Claude API로 두 가지 샘플 요청("로그인 500 에러", "중복 결제")을
분류해봤다. 각각 BUG/URGENT, BILLING/URGENT로 정확히 분류됐고 confidence 0.98, 판단 근거도
합리적이었다.
