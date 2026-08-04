# 0008. GitHub Actions CI 파이프라인

- 날짜: 2026-08-04
- 상태: 채택됨

## 배경

모노레포(backend/triage-service/frontend) 각각의 빌드+테스트를 push/PR마다 자동으로
돌리기 위해 GitHub Actions를 도입했다.

## 결정

### 워크플로우를 디렉토리별로 3개 분리 (`backend-ci.yml` / `triage-service-ci.yml` / `frontend-ci.yml`)

하나의 워크플로우에서 3개 잡을 다 돌리는 대신, 각 워크플로우가 자기 디렉토리
(`paths:`) 변경에만 반응하도록 분리했다. 프론트엔드만 고친 커밋에서 백엔드
빌드까지 도는 건 불필요한 CI 비용/시간 낭비라서 — 이 프로젝트 전체를 관통하는
"필요할 때만 비용을 쓴다"는 Gate 설계 철학(불필요한 LLM 호출을 걸러내는 것과
같은 맥락)을 CI에도 그대로 적용했다.

### Backend: Testcontainers를 CI에서도 별도 설정 없이 그대로 사용

`./mvnw test`만 실행하면 된다. `AbstractIntegrationTest`(0007)가 테스트 시작 시
직접 Postgres 컨테이너를 띄우고, `ubuntu-latest` 러너에는 Docker가 기본으로 깔려
있어서 별도 서비스 컨테이너나 docker-compose 설정이 필요 없다.

### Triage service: 아직 pytest가 없어서 "임포트가 되는지"만 확인

FastAPI 앱에 자동화된 테스트를 아직 작성하지 않았다(스코프 밖 — Task #6에서는
Claude API 연동을 수동으로만 확인했다). 테스트를 없는데 있는 척 채우기보다,
지금 시점에서 실제로 의미 있는 최소 체크만 두기로 했다: `ANTHROPIC_API_KEY`가
없으면 `Settings()`가 아예 인스턴스화에 실패하는 걸 이용해, 더미 값으로 앱
임포트가 정상적으로 되는지만 확인한다. 나중에 pytest를 추가하면 이 워크플로우에
테스트 스텝만 끼워 넣으면 된다.

### Frontend: 자동 테스트 없이 `lint` + `build`(=`tsc -b`)만

Vitest 등 프론트엔드 테스트 러너를 아직 붙이지 않아서, 타입 체크(`tsc -b`)와
빌드 성공 여부, oxlint 통과 여부로 최소한의 회귀를 잡는다.

## 삽질 기록

**`backend/mvnw`가 git에 실행 권한(`100644`) 없이 커밋돼 있었다.** Windows에서
작업하다 보니 눈치채기 어려웠는데, 그대로 뒀으면 Linux CI 러너에서
`Permission denied`로 첫 스텝부터 실패했을 것. `git update-index --chmod=+x
backend/mvnw`로 인덱스의 파일 모드만 바꿔서 고쳤다(파일 내용은 그대로).

## 검증

세 워크플로우가 실행할 커맨드(`./mvnw test`, `npm run lint && npm run build`,
더미 키로 앱 임포트)를 로컬에서 그대로 실행해 전부 통과하는 것을 미리 확인한
뒤 워크플로우 파일을 작성했다. 실제 GitHub Actions 실행 결과는 push 후
Actions 탭에서 확인한다.
