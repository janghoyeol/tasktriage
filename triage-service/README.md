# triage-service

Gate 2 분류 서비스. Task의 제목/설명을 받아 Claude API(Haiku 4.5)로 카테고리, 긴급도, confidence score를 판단해 반환한다. Gate 1(규칙 기반 필터링)과의 연결은 백엔드(Spring Boot)에서 처리한다 — 자세한 구조는 [../CLAUDE.md](../CLAUDE.md) 참고.

## 로컬 실행

```bash
python -m venv .venv
.venv\Scripts\activate       # Windows
pip install -r requirements.txt
cp .env.example .env         # ANTHROPIC_API_KEY 채워넣기
uvicorn app.main:app --reload --port 8001
```

## 엔드포인트

- `GET /health` — 헬스체크
- `POST /classify` — `{title, description}` → `{category, urgency, confidence, reasoning}`

Claude의 구조화 출력(`output_config.format`)으로 응답 스키마를 강제하기 때문에 파싱 실패를 별도로 처리하지 않는다.
