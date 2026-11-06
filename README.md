# GIEOKHAM 2 · 기억함

<p align="center">
  <img src="https://raw.githubusercontent.com/pabu-lim/gieokham/main/docs/gieokham-brand.png" alt="기억함 — 나의 작은 기억 보관함" width="210">
</p>

<p align="center">
  <strong>머릿속은 가볍게, 중요한 일은 또렷하게.</strong><br>
  Keep your thoughts here. Focus on what matters.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-437291?style=flat-square" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=flat-square" alt="Spring Boot 4.1.1">
  <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square" alt="Spring Data JPA">
  <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1?style=flat-square" alt="PostgreSQL 17">
  <img src="https://img.shields.io/badge/status-in_development-orange?style=flat-square" alt="개발 진행 중">
</p>

떠오른 생각을 붙잡고, 해야 할 일을 정리하고, 지금 중요한 일에 집중하세요.

**기억함2는 직접 사용해 온 기억함의 경험을 Java · Spring Boot · PostgreSQL로 다시 만드는 프로젝트입니다.**
빠르게 기록하고 편하게 꺼내 보는 사용 경험을 이어 가면서, API부터 데이터 모델과 배포까지 직접 설계하고 구현합니다.

> **현재 단계 · React 프론트엔드 연결 구현**
> 실제 백엔드 API에 맞춘 화면을 추가했습니다. 프론트 빌드·자동 테스트·체험 화면은 검증했으며, Google 로그인과 DB를 포함한 종단 검증은 로컬에서 이어갑니다. 아직 운영 배포하지 않았습니다.

[프론트 실행 및 검증 안내](docs/frontend.md) · [1세대 소스](https://github.com/pabu-lim/gieokham)

![기억함2 홈 화면 — 체험 데이터](docs/frontend-home.jpg)

## 주요 기능

### 머릿속 생각을 바로 기록

Enter로 빠르게 저장하고 Shift+Enter로 줄을 바꿉니다. 카테고리와 중요도는 입력창에서 함께 선택하고, 카테고리 안에서는 해당 분류가 미리 선택됩니다.

### 지금 챙길 기억을 한눈에

홈에서 임박한 일정·기한이 지난 일·중요한 메모를 먼저 확인하고, 최근 남긴 기억을 함께 봅니다. PC는 두 열, 모바일은 한 열로 보여줍니다.

### 나에게 맞는 정리 방식

5단계 중요도, 하위 카테고리, 검색, 커스텀·중요도·최신순 정렬을 지원합니다. 메모와 카테고리는 드래그로 정리하고, 카테고리를 접거나 펼칠 수 있습니다. 카테고리 삭제 시 하위 카테고리와 메모도 함께 삭제합니다.

### 일정부터 반복 습관까지

날짜만 정하거나 시작·종료 일시를 지정합니다. 일정 없이 반복만 설정할 수도 있습니다. 매일·선택 요일·매월·매년 반복과 날짜별 완료 기록, 완료 취소를 지원합니다.

### 내 계정에 안전하게

Google 인증과 명시적인 서비스 가입을 분리합니다. 세션과 CSRF 검증을 사용하고, 서버에서 회원별 소유권을 확인합니다. 로그아웃·세션 만료 시 화면과 암호화 사본을 비웁니다.

## 구성

| 영역 | 기술 및 역할 |
| --- | --- |
| 화면 | React 19 · TypeScript · Vite · 반응형 웹/PWA |
| API | Java 17 · Spring Boot 4.1.1 |
| 인증 | Spring Security · Google OIDC · 서버 세션 |
| 데이터 | PostgreSQL 17 · Spring Data JPA |
| 시작 사본 | IndexedDB · AES-GCM · 추출 불가능한 키 · 24시간 만료 |
| 배포 | React 정적 빌드를 Spring JAR에 포함 가능 |

`gieokham-web`과 `gieokham-api`를 개발 중 별도로 실행합니다. 운영에서는 JAR 하나로 웹·모바일 화면과 API를 제공할 수 있습니다. 상세 명령어와 IntelliJ 환경변수는 [실행 안내](docs/frontend.md)에 있습니다.

## 데이터 구조

| 테이블 | 내용 |
| --- | --- |
| `member` | 회원 이름·닉네임·대표 이메일 |
| `member_identity` | 공급자와 공급자 계정 ID로 회원에 로그인 연결 |
| `category` | 회원별 카테고리·상위 카테고리·순서 |
| `memo` | 내용·중요도·카테고리·일정·반복 규칙·커스텀 순서 |
| `memo_repeat_day` | 매주 반복 요일 |
| `memo_completion` | 반복 메모의 날짜별 완료 기록 |

미분류는 `memo.category_id IS NULL`입니다. 일반 메모 완료는 `completed_at`, 반복 완료는 별도 기록으로 처리합니다. 날짜·시간은 한국 시간 기준이며, 응답 변환은 서비스 트랜잭션 안에서 수행합니다.

스키마는 `gieokham-api/src/main/resources/db/schema.sql`에 있습니다. `local` 프로필은 시작 시 `db/local/reset.sql`로 초기화한 뒤 테스트 데이터를 생성합니다. **실제 기록을 보관할 DB에는 local 프로필을 사용하지 않습니다.**

## API

| 경로 | 기능 |
| --- | --- |
| `/api/auth/me`, `/csrf`, `/signup`, `/logout` | 세션 확인·요청 토큰·가입·로그아웃 |
| `/api/categories` | 카테고리 조회·생성 |
| `/api/categories/{id}`, `/{id}/move` | 수정·하위 포함 삭제·이동 |
| `/api/memos` | 생성·검색·필터·페이지 조회 |
| `/api/memos/{id}` | 상세·수정·삭제 |
| `/api/memos/{id}/completion`, `/{id}/move` | 완료 상태·커스텀 이동 |
| `/api/memos/today`, `/completions/today` | 오늘 할 일·오늘 완료한 반복 메모 |
| `/api/memos/{id}/completions` | 반복 완료 이력 |
| `/api/memos/{id}/completions/{scheduledDate}` | 해당 날짜 완료 취소 |

## 현재 검증과 남은 작업

프론트 자동 테스트 13개와 운영 빌드가 통과했습니다. PC와 390px 화면에서 체험 데이터를 이용해 입력·수정·반복 완료·메뉴 전환을 확인했습니다. 실제 Google 로그인·PostgreSQL 연동, 갤럭시 터치와 설치 검증은 남아 있습니다.

보류한 항목은 **통합 완료 목록, 신규 가입 기본 데이터, 백업·불러오기**입니다. 반복 완료 이력은 상세에서 확인할 수 있습니다. 기존 기억함의 데이터 자동 이전은 하지 않습니다.

기억함1 저장소와 운영 서비스는 유지합니다. 이 브랜치는 검토용이며 main 병합과 배포는 별도입니다.
