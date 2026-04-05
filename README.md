# Shathing Backend

지역 기반 공유 서비스 `Shathing`의 백엔드 서버입니다.  
사용자 인증, 지역/카테고리 마스터 데이터, 공유글 CRUD, 이미지 업로드, 1:1 채팅까지 서비스의 핵심 흐름을 Spring Boot로 구현했습니다.

## Project Summary

- **Goal**: 지역 단위로 물품을 공유하고, 서비스 안에서 바로 대화를 이어갈 수 있는 백엔드 구축
- **Stack**: Java 17, Spring Boot 4, Spring Security, OAuth2, JPA, PostgreSQL, WebSocket/STOMP
- **Storage**: Cloudflare R2 Presigned Upload
- **Deployment Target**: Docker, Cloud Run
- **Chat Strategy**: MVP 단계에서는 단일 인스턴스 전제로 Simple Broker 사용

## Why This Project

중고 거래나 빌림/공유 서비스는 결국 아래 흐름이 자연스럽게 이어져야 합니다.

1. 사용자가 로그인한다.
2. 지역과 카테고리를 기준으로 글을 탐색한다.
3. 이미지는 빠르게 업로드된다.
4. 관심 있는 사용자끼리 바로 대화를 시작한다.

이 프로젝트는 그 핵심 흐름을 안정적으로 제공하는 백엔드 API를 만드는 데 집중했습니다.

## Key Features

### 1. Authentication

- 이메일 인증 기반 로그인
- Google OAuth 로그인
- JWT `accessToken` / `refreshToken` 쿠키 인증
- `/me` 기반 사용자 정보 조회

### 2. Region & Category Master Data

- KR / US 지역 데이터를 `Region` 트리로 관리
- 카테고리를 국가별 표시명으로 응답
- 앱 시작 시 조건부 마스터 데이터 적재 지원

### 3. Shared Item

- 공유글 생성, 수정, 삭제
- 카테고리 / 지역 / 검색어 기반 목록 조회
- 작성자 본인만 수정/삭제 가능하도록 권한 검증

### 4. Upload

- Cloudflare R2에 직접 업로드할 수 있도록 Presigned URL 발급
- 애플리케이션 서버를 파일 중계 서버로 쓰지 않도록 분리

### 5. Chat

- 1:1 채팅방 생성
- 채팅방 목록 조회
- 메시지 목록 커서 조회
- WebSocket + STOMP 기반 실시간 메시지 전송

## Architecture

```text
controller -> service -> repository -> database
```

주요 도메인:

- `Member`
- `Category`
- `Region`
- `SharedItem`
- `ChatRoom`
- `ChatMessage`

패키지 구조:

```text
src/main/java/com/shathing/backend
├── common
├── config
├── controller
├── dto
├── entity
├── exception
├── repository
└── service
```

## Technical Decisions

### JWT를 쿠키로 관리

토큰을 로컬 스토리지에 두는 대신 쿠키 기반으로 처리했습니다.  
브라우저 기반 서비스에서 인증 상태를 다루기 단순하고, refresh 흐름과 Google OAuth redirect 처리도 자연스럽게 연결됩니다.

### Region 모델을 `LegalDong`에서 일반화

초기에는 한국 법정동 전용 구조였지만, 여러 국가를 지원하기 위해 `Region(id, countryCode, parentId, depth, name)` 구조로 단순화했습니다.  
덕분에 한국/미국 데이터를 같은 API로 조회할 수 있게 정리했습니다.

### 업로드는 서버가 아닌 스토리지로 직접 전송

이미지 파일을 애플리케이션 서버가 직접 받지 않고, R2 Presigned URL을 발급해 클라이언트가 곧바로 업로드하도록 설계했습니다.  
트래픽과 서버 부하를 줄이고, 업로드 책임을 분리하는 데 유리합니다.

### 채팅은 MVP에 맞춰 단일 인스턴스 전제

Cloud Run 다중 인스턴스 환경에서 WebSocket fan-out까지 바로 풀면 Redis 같은 중간 계층이 필요해집니다.  
현재는 MVP 범위에 맞춰 Spring Simple Broker 기반으로 구현했고, 단일 인스턴스 운영을 전제로 두었습니다.

## Implementation Highlights

### 공유글 검색

- `GET /share/posts`
- `categoryId`, `regionId`, `countryCode`, `search`, `page`, `size` 조합 지원
- `search`는 제목 + 본문 기준 부분 일치 검색
- `Specification` 기반 동적 필터 구성

### 지역 데이터 로더

- 한국 CSV, 미국 Gazetteer 파일을 startup 시 적재
- `save()` 반복 호출 대신 배치 insert 방식으로 조정
- `country_code` 기준으로 KR / US 적재 여부를 나눔

### 채팅 메시지 흐름

- REST로 채팅방 생성/조회
- STOMP로 실시간 송신
- `GET /chat/rooms/{roomId}/messages`는 커서 기반 조회
- WebSocket handshake 시 JWT 쿠키를 검증해 사용자 식별

### 공통 에러 응답

- `400`, `401`, `403`, `404`, `405`, `500`
- JSON 형태로 통일된 에러 응답 사용

## API Highlights

### Auth

- `POST /auth/send-email`
- `POST /auth/verify-token`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /me`
- `GET /oauth2/authorization/google`

### Category / Region

- `GET /categories?countryCode=KR`
- `GET /category?categoryId=1&countryCode=US`
- `GET /regions?countryCode=KR&search=강남`
- `GET /region?regionId=1`

### Shared Item

- `POST /share/post`
- `PUT /share/posts/{id}`
- `DELETE /share/posts/{id}`
- `GET /share/posts`
- `GET /share/posts/{id}`

### Upload

- `POST /uploads/presigned-url`

### Chat

- `POST /chat/rooms`
- `GET /chat/rooms`
- `GET /chat/rooms/{roomId}/messages`

STOMP:

- endpoint: `/ws-chat`
- publish: `/pub/chat/rooms/{roomId}/messages`
- subscribe: `/topic/chat/rooms/{roomId}`

## Run Locally

### Requirements

- JDK 17
- PostgreSQL
- Gmail SMTP 계정
- Google OAuth Client
- Cloudflare R2 버킷

### Local Config

로컬 설정은 `src/main/resources/application-local.yaml`에 있습니다.

현재 코드 기준 주요 설정 키:

- `spring.datasource.*`
- `spring.mail.*`
- `spring.security.oauth2.client.registration.google.*`
- `jwt.*`
- `auth.token-expiration-seconds`
- `app.frontend-url`
- `app.category.initialize-on-startup`
- `app.region.initialize-on-startup`
- `cookie.*`
- `r2.*`

### Run

```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/*.jar
```

## Data Initialization

카테고리와 지역 데이터는 필요할 때만 startup 시 적재할 수 있습니다.

- `app.category.initialize-on-startup: true`
- `app.region.initialize-on-startup: true`

사용하는 리소스:

- `src/main/resources/data/국토교통부_전국 법정동_20250807.csv`
- `src/main/resources/data/2025_Gaz_state_national.txt`
- `src/main/resources/data/2025_Gaz_counties_national.txt`

운영 환경에서는 매 배포 시 자동 적재보다, 1회성 작업으로 분리하는 편이 더 안전합니다.

## Deployment Notes

- `Dockerfile`은 Gradle `bootJar` 기반 멀티 스테이지 빌드입니다.
- 프록시 환경에서는 `server.forward-headers-strategy: framework`를 사용합니다.
- Google OAuth Redirect URI는 배포 도메인과 정확히 일치해야 합니다.
- WebSocket 채팅을 유지하려면 현재는 인스턴스 수를 1로 제한하는 편이 안전합니다.

## What I Focused On

- 기능 구현보다 서비스 흐름이 끊기지 않는 API 설계
- 인증, 검색, 업로드, 채팅을 한 서비스 안에서 일관되게 연결하는 구조
- MVP에 맞는 현실적인 설계 선택
- 이후 Redis, 프로필 분리, 운영용 시크릿 관리로 확장 가능한 기반 마련
