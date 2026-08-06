# 0011. Render 실배포 중 겪은 문제 3가지

- 날짜: 2026-08-05
- 상태: 채택됨

## 배경

Render + Neon으로 실제 배포(ADR 0009)를 진행하면서, Dockerfile/docker-compose(ADR 0010)
단계에서는 안 드러났던 문제 세 가지를 만났다. 셋 다 로컬에선 재현이 안 되고 실제
플랫폼에 올려야만 드러나는 종류라 따로 기록한다.

## 삽질 1: frontend Static Site Build Command가 비어서 배포 실패

첫 배포(Static Site 생성 직후 자동 트리거된 배포)가 "Exited with status 1"로
실패했다. Render 대시보드의 배포 목록(Events)에는 이 한 줄만 보여서 원인을 알 수
없었고, **실패한 배포를 직접 열어 빌드 로그 전체**를 봐야 진짜 원인이 나왔다:

```
==> Empty build command; skipping build
==> Publish directory dist does not exist!
```

Static Site 생성 화면에서 Build Command에 `npm run build`를 입력했다고 생각했는데
실제로는 저장이 안 됐던 것 — Settings 탭에서 다시 채워 넣고 수동 재배포해서 해결했다.

**교훈**: 배포 실패는 "성공/실패" 상태만 보고 판단하면 안 되고, 반드시 실제 빌드
로그를 열어서 봐야 한다. "Exited with status 1"이라는 메시지 하나로는 의존성 설치
실패, 컴파일 에러, 설정 누락 등 원인이 전혀 구분이 안 된다.

## 삽질 2: `_redirects` 파일이 200 rewrite가 아니라 301 redirect로 동작

React Router의 클라이언트 사이드 라우팅을 위해 Netlify 스타일 `frontend/public/_redirects`
파일(`/*    /index.html   200`)을 추가했다. 루트(`/`)는 정상 동작했는데, `/tasks`처럼
하위 경로로 직접 접속하면 **301로 `/index.html`로 리다이렉트**되면서 URL 자체가
바뀌어버렸다 — 이러면 React Router가 원래 의도한 경로(`/tasks`)를 못 읽어서 SPA
라우팅이 완전히 깨진다.

Render 공식 문서(`render.com/docs/redirects-rewrites`)를 직접 확인해보니, Render는
SPA rewrite를 **대시보드에서 Source/Destination/Action(Rewrite)을 직접 등록**하는
방식으로 지원하고, `_redirects` 파일 기반 설정은 공식 문서에 아예 없었다. 파일을
지우고 대시보드에 `/* → /index.html, Rewrite`로 규칙을 등록하니 `/tasks`가 URL을
유지한 채 200으로 정상 응답했다.

**교훈**: 한 플랫폼(Netlify)의 관례가 다른 플랫폼에서도 통할 거라고 가정하면
안 된다. "Render도 `_redirects`를 지원한다"는 커뮤니티 언급만 믿지 않고 공식 문서를
직접 확인해서 진짜 지원 방식을 찾았다.

## 삽질 3: CORS 설정을 안 갱신한 채로 방치해서 접속 실패

3개 서비스가 서로의 URL을 필요로 하는 순환 의존 구조라(frontend는 빌드 시점에
backend URL이 필요하고, backend는 CORS 허용을 위해 frontend URL이 필요함) 배포
순서를 **triage-service → backend(CORS는 임시로 localhost) → frontend → backend
CORS를 실제 frontend URL로 갱신**으로 잡았는데, 마지막 단계를 바로 안 하고
자리를 비운 사이 실제 배포된 프론트에서 API 호출이 전부 CORS로 막혔다.

여기에 무료 티어 콜드스타트(15분 무활동 후 슬립, ADR 0009)까지 겹쳐서 증상이
헷갈렸다 — 콜드스타트는 시간이 지나면 저절로 풀리지만, CORS 차단은 설정을 직접
고치지 않는 한 아무리 기다려도 안 풀린다는 차이가 있다. `CORS_ALLOWED_ORIGINS`를
실제 frontend URL로 갱신하고 나서야 완전히 해결됐다.

**교훈**: 서비스 간 순환 의존이 있는 배포는 "마지막 갱신 단계"를 빼먹기 쉽다.
증상이 여러 원인이 겹쳐서 나타날 때는 "시간이 지나면 저절로 풀리는 것"과 "설정을
고쳐야만 풀리는 것"을 구분해서 봐야 한다.

## 검증

세 문제 모두 고친 뒤, 브라우저로 실제 배포된 URL(https://tasktriage-frontend.onrender.com)
에서 회원가입 → 로그인 → Task 등록(Gate 1→2 실제 Claude API 호출) → 대시보드 →
상태 전이까지 전체 플로우를 처음부터 다시 수행해 전부 정상 동작하는 것을 확인했다.
