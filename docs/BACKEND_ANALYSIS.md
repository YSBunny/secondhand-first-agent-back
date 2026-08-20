# Secondhand First 백엔드 프로젝트 분석

> 분석 기준: 현재 `feat/mypage-backend` 브랜치의 실제 소스 코드  
> 분석일: 2026-08-20  
> 원칙: 구현 여부는 Controller 존재만이 아니라 Service·Repository·DB·외부 연동까지 추적하여 판정한다.

## 1. 프로젝트 개요

Secondhand First 백엔드는 여러 중고거래 플랫폼의 상품을 AI 검색 결과로 정규화하고, Best Deal·상품 활동·마이페이지 데이터를 제공하는 Spring Boot 애플리케이션이다.

| 항목 | 현재 구성 |
| --- | --- |
| 언어/런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.0.7 |
| 빌드 | Gradle |
| 웹/API | Spring MVC, Bean Validation |
| 인증 | Spring Security, 자체 HS256 JWT, BCrypt |
| 영속성 | Spring Data JPA, MySQL(local), H2(test/runtime 의존성) |
| 외부 연동 | Kakao Local REST API, AI 검색 연동 인터페이스 |
| 캐시/세션 | Redis starter는 있으나 실제 사용 코드는 없음 |
| 응답 규격 | `ApiResponse<T>(success, message, data, timestamp)` |

설정은 `application.yaml`에서 선택적 `.env`를 import한다. DB URL·계정, JWT, CORS, Kakao API 정보를 환경 변수로 주입하고 JPA의 `ddl-auto`는 `update`, `open-in-view`는 `false`이다.

## 2. 패키지 구조

```text
com.hackathon.second_hand_first
├── activity
│   ├── controller   # 마이페이지 대시보드
│   ├── domain       # 탄소 미션, 상품 조회, 플랫폼 이동 기록
│   ├── dto
│   ├── exception
│   ├── repository
│   └── service
├── auth
│   ├── controller
│   ├── dto
│   ├── exception
│   ├── security     # JWT 필터, Security 설정
│   ├── service
│   └── token        # RefreshToken, TokenProvider
├── common
│   ├── config       # CORS/시간/로컬 데이터 초기화
│   └── response     # 공통 응답 및 전역 예외 처리
├── location
│   ├── config
│   ├── controller
│   ├── dto
│   ├── exception
│   └── service      # Kakao Local 연동
├── product
│   ├── config       # local fixture
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── exception
│   ├── repository
│   └── service
├── search
│   ├── application  # AiSearchClient 계약
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── exception
│   ├── infrastructure # 미설정 AI 클라이언트
│   ├── integration/ai # 임시 AI 요청/응답 DTO
│   ├── repository
│   └── service
└── user
    ├── controller
    ├── domain
    ├── dto
    ├── repository
    └── service
```

`activity/* 2`, `product/* 2`, `search/* 2`처럼 이름 끝에 공백과 `2`가 붙은 빈 디렉터리가 존재한다. 컴파일에는 영향을 주지 않지만 병합/파일 복사 잔재로 보이므로 정리가 필요하다.

## 3. 도메인별 구성

| 도메인 | 책임 | 핵심 클래스 | 현재 상태 |
| --- | --- | --- | --- |
| Auth | 가입, 로그인, 토큰 갱신/폐기, 비밀번호 변경 | `AuthController`, `AuthService`, `TokenProvider`, `RefreshToken` | ✅ |
| User | 내 프로필 조회/수정/탈퇴, 위치 저장 | `UserController`, `UserService`, `User` | ✅ |
| Location | 주소 검색, 상품 주소 좌표 변환 | `LocationSearchController`, `KakaoLocalService`, `ProductLocationGeocodeService` | ✅ 외부 키 필요 |
| Product | 상품/이미지/판매자, Best Deal, 거리 정렬, upsert | `Product`, `ProductImage`, `Seller`, `BestDealService`, `ProductUpsertService` | ⚠️ 일부 임시 로직 |
| Search | AI 검색 세션·조건·메시지·결과 저장/조회 | `SearchSessionController`, `SearchSessionService`, `AiSearchClient` | ⚠️ 생성 API 실사용 불가 |
| Activity | 조회 미션, 플랫폼 이동, 마이페이지 통계 | `CarbonQuestService`, `PlatformRedirectService`, `UserDashboardService` | ✅ |

## 4. 전체 REST API 목록

Security 설정상 가입·로그인·토큰 갱신만 공개이며 나머지 API는 Bearer access token이 필요하다.

| # | Method | Endpoint | Controller | 인증 | 판정 |
| ---: | --- | --- | --- | --- | --- |
| 1 | POST | `/auth/signup` | `AuthController` | 공개 | ✅ |
| 2 | POST | `/auth/login` | `AuthController` | 공개 | ✅ |
| 3 | POST | `/auth/token/refresh` | `AuthController` | refresh cookie | ✅ |
| 4 | POST | `/auth/logout` | `AuthController` | 필요 | ✅ |
| 5 | PATCH | `/auth/password` | `AuthController` | 필요 | ✅ |
| 6 | GET | `/users/me` | `UserController` | 필요 | ✅ |
| 7 | PATCH | `/users/me` | `UserController` | 필요 | ✅ |
| 8 | DELETE | `/users/me` | `UserController` | 필요 | ⚠️ 연관 데이터 정책 필요 |
| 9 | PATCH | `/users/me/location` | `LocationController` | 필요 | ✅ |
| 10 | GET | `/locations/search` | `LocationSearchController` | 필요 | ✅ |
| 11 | POST | `/locations/geocode` | `LocationSearchController` | 필요 | ✅ |
| 12 | GET | `/products/best-deals` | `BestDealController` | 필요 | ⚠️ 임시 추천 점수 |
| 13 | GET | `/users/me/dashboard` | `UserDashboardController` | 필요 | ✅ |
| 14 | GET | `/users/me/carbon-quest` | `ProductActivityController` | 필요 | ✅ |
| 15 | POST | `/products/{productId}/views` | `ProductActivityController` | 필요 | ✅ |
| 16 | POST | `/products/{productId}/redirect` | `ProductActivityController` | 필요 | ✅ |
| 17 | POST | `/products/rank-by-distance` | `ProductDistanceController` | 필요 | ⚠️ 비정형 JSON |
| 18 | POST | `/search-sessions` | `SearchSessionController` | 필요 | ❌ 항상 502 |
| 19 | GET | `/users/me/search-sessions` | `SearchSessionController` | 필요 | ✅ |
| 20 | GET | `/search-sessions/{sessionId}` | `SearchSessionController` | 필요 | ✅ |

> 표 번호는 20개이다. 인증 5개, 사용자/위치 6개, 상품/활동 6개, 검색 3개로 구성된다.

## 5. API 상세 분석

### 5.1 인증 API

#### `POST /auth/signup`

- 흐름: `AuthController.signup` → `AuthService.signup` → `UserRepository.save`
- 입력: 이름, 이메일, 비밀번호, 약관/마케팅 동의, 선택 프로필 이미지
- 처리: 이메일 중복 검사, BCrypt 암호화, `User` 생성
- 출력: `UserSummaryResponse`, HTTP 201
- 주의: 이메일은 Entity 생성 시 trim/lowercase 처리된다.

#### `POST /auth/login`

- 흐름: Controller → `AuthService.login` → `UserRepository`/`PasswordEncoder` → `TokenProvider` → `RefreshTokenRepository`
- 처리: access token 생성, refresh token DB upsert, HttpOnly/SameSite=Lax cookie 발급
- 입력의 `rememberMe`에 따라 refresh cookie 유지 시간이 결정된다.
- ⚠️ 가입 시 이메일은 정규화하지만 로그인 조회 전에는 동일한 명시적 정규화가 없어 대소문자/공백 입력 정책을 통일해야 한다.

#### `POST /auth/token/refresh`

- cookie의 refresh token 서명·만료·type과 DB 저장값을 모두 검증한다.
- 성공 시 access/refresh token을 재발급하고 DB 토큰 및 cookie를 교체한다.

#### `POST /auth/logout`, `PATCH /auth/password`

- 로그아웃은 해당 사용자의 DB refresh token을 삭제하고 cookie를 만료시킨다.
- 비밀번호 변경은 기존 비밀번호 확인 및 새 비밀번호 중복 방지 후 BCrypt 값으로 갱신한다.

### 5.2 사용자·지역 API

#### `GET/PATCH/DELETE /users/me`

- 흐름: `UserController` → `UserService` → `UserRepository`
- 조회 응답은 사용자 ID, 이름, 이메일, 프로필 이미지, 가입일, 위치를 포함한다.
- 수정은 프로필 필드를 갱신한다.
- 탈퇴는 refresh token과 사용자 행을 삭제한다.
- ⚠️ 검색/활동 테이블 다수가 `userId` scalar를 가지며 DB FK/cascade가 없다. 탈퇴 시 개인정보 삭제·익명화·보존 정책과 정리 작업이 필요하다.

#### `PATCH /users/me/location`

- `KakaoLocalService`로 지역 후보를 해석한 뒤 `User.updateLocation`으로 region/위도/경도를 저장한다.

#### `GET /locations/search`, `POST /locations/geocode`

- 흐름: Controller → Kakao 관련 Service → Spring `RestClient` → Kakao Local API
- 주소 후보 검색과 상품 위치 정규화/좌표화를 담당한다.
- Kakao 오류는 `KakaoLocalException`의 status를 그대로 응답한다.
- 실행 시 `KAKAO_REST_API_KEY`가 반드시 필요하다.

### 5.3 상품·활동 API

#### `GET /products/best-deals`

- 흐름: `BestDealController` → `BestDealService` → `ProductRepository`
- 판매 중 상품을 이미지·판매자와 조회하고 category 필터, 추천/가격 정렬, 페이지 슬라이싱을 메모리에서 수행한다.
- 출력: 상품 ID, 순위, 플랫폼, 카테고리, 가격/절약액, 추천 근거, 이미지 등.
- ⚠️ 추천 점수는 AI 계약 전 임시 계산식이며 `isFavorite`은 항상 `false`이다.
- ⚠️ 전체 후보 로딩 후 메모리 페이징이라 데이터 증가 시 성능 문제가 발생한다.

#### `POST /products/rank-by-distance`

- 흐름: `ProductDistanceController` → `ProductDistanceRankingService`
- 사용자의 저장 좌표와 요청 JSON 내부 위치를 haversine 공식으로 계산해 `distance_km`, 순위, 최근접 좌표를 JSON에 추가한다.
- ⚠️ `JsonNode/ObjectNode` 기반 비정형 계약이어서 검증·문서화·타입 안정성이 약하다.

#### 탄소 미션

- `GET /users/me/carbon-quest`: 서울 날짜 기준 당일 미션을 조회하거나 0 상태를 반환한다.
- `POST /products/{id}/views`: 사용자 pessimistic lock 후 상품 존재/탄소 대상 여부/당일 중복을 확인하고 조회 기록과 미션을 갱신한다.
- `(user_id, product_id, viewed_date)` unique 제약으로 같은 날 같은 상품 중복 집계를 방지한다.
- 목표 3건 달성 시 100P 완료 상태를 기록한다.

#### `POST /products/{id}/redirect`

- 상품의 원본 URL이 HTTPS이고 플랫폼별 허용 host인지 검증한 뒤 이동 이력을 저장한다.
- 응답은 플랫폼, redirect URL, 서울 기준 시각이다.
- 모든 클릭을 보존하며 대시보드는 누적 행 수를 사용한다.

### 5.4 검색 API

#### `POST /search-sessions`

- 의도 흐름: Controller → `SearchSessionService.create` → `AiSearchClient.search` → `ProductUpsertService` → 검색 Repository들.
- AI 결과가 오면 세션, 조건, 사용자/AI 메시지, 정규화 상품·판매자·이미지, 순위 결과를 transaction으로 저장한다.
- ❌ 현재 실제 Bean인 `UnconfiguredAiSearchClient`는 항상 `AiServerUnavailableException`을 던져 HTTP 502로 끝난다.

#### `GET /users/me/search-sessions`

- 사용자 소유 세션을 수정 시각 역순으로 페이지 조회한다.
- 키워드, query summary, 마지막 메시지, 결과 수, 갱신 시각을 응답한다.

#### `GET /search-sessions/{sessionId}`

- 세션 ID와 인증 사용자 ID를 함께 검증해 다른 사용자의 세션 접근을 막는다.
- 원 질문, 파싱 조건, 시간순 메시지를 복원한다.
- 현재 메시지 DTO에 role이 없어 UI가 발신 주체를 구분해야 한다면 계약 보강이 필요하다.

## 6. Entity·DTO 매핑

| Entity | 주요 요청/응답 DTO | 용도 |
| --- | --- | --- |
| `User` | `SignupRequest`, `LoginRequest`, `UserProfileResponse`, `UserProfileUpdateRequest` | 계정/프로필/좌표 |
| `RefreshToken` | `LoginResponse`, `TokenRefreshResponse` | refresh token DB 저장 및 교체 |
| `Product` | `BestDealItemResponse`, AI 임시 상품 DTO | 플랫폼 상품 정규화 |
| `ProductImage` | `imageUrl` 필드 | 정렬된 다중 상품 이미지 |
| `Seller` | Best Deal/AI 상품 필드 | 판매자 신뢰도·거래 수 |
| `SearchSession` | `SearchSessionCreateResponse`, `RecentSearchSessionResponse`, `SearchSessionDetailResponse` | 검색 단위 상태/요약 |
| `SearchCondition` | `ParsedConditionsResponse` | AI가 해석한 조건 |
| `SearchMessage` | `SearchMessageResponse` | 검색 화면 대화 기록 |
| `SearchResult` | 생성 응답의 resultCount, 내부 저장 | 세션별 상품 순위/추천 정보 |
| `CarbonQuest` | `CarbonQuestResponse` | 사용자별 일일 진행도·보상 |
| `ProductViewRecord` | `ProductViewResponse` | 상품별 일일 중복 방지 |
| `PlatformRedirectHistory` | `PlatformRedirectResponse`, `DashboardStatsResponse` | 외부 플랫폼 이동 기록/통계 |

공통 응답은 API 명세의 단순 `{message,data}`가 아니라 실제로 `success`와 `timestamp`도 포함한다. 프론트 타입 및 Notion 명세를 실제 규격과 일치시켜야 한다.

## 7. 핵심 비즈니스 로직

### 인증

1. 비밀번호는 BCrypt로만 저장한다.
2. access token 기본 수명은 3,600초, refresh token은 1,209,600초이다.
3. refresh token은 사용자당 1개를 관계형 DB에 저장한다.
4. 로그아웃·비밀번호 변경 시 refresh token 정책을 일관되게 확인해야 한다.

### Best Deal

1. `SELLING` 상품을 대상으로 한다.
2. 카테고리 필터를 적용한다.
3. 가격, 상태, 판매자 신뢰도, 절약률을 사용한 임시 점수로 정렬한다.
4. 서비스 메모리에서 순위와 페이지를 구성한다.

### 탄소 미션

1. 서울 시간의 날짜가 미션 기준이다.
2. 탄소 절감 대상 상품만 집계한다.
3. 동일 사용자·상품·날짜는 한 번만 센다.
4. 하루 3개 서로 다른 상품 조회가 목표이고 완료 시 100P다.
5. `CarbonQuest`는 매일 초기화 행을 덮어쓰는 방식이 아니라 날짜별 이력으로 보존된다.

### 플랫폼 이동

1. 상품 원본 URL의 scheme/host를 검증한다.
2. 상품 플랫폼과 허용 도메인이 일치해야 한다.
3. 검증 후 이동 기록을 저장하고 누적 통계에 반영한다.

### AI 검색 저장

AI가 자연어 분석·외부 탐색·추천 순위까지 반환한다는 전제다. 백엔드는 결과를 검증/정규화하고 `(platform, externalProductId)` 기준으로 상품을 upsert한 후 세션과 결과를 저장한다.

## 8. DB 구조와 Repository

| Table/Entity | 주요 키·제약 | Repository 사용 |
| --- | --- | --- |
| `users` | PK id, email unique/index | 이메일/ID 조회, 계정 저장/삭제 |
| `refresh_tokens` | PK id, userId unique | 사용자별 토큰 조회/upsert/delete |
| `products` | `(platform, external_product_id)` unique, 상태/가격/게시일 index | 상품 조회, 판매 상품 조회, upsert |
| `product_images` | 상품/표시 순서 제약 및 product FK | Product cascade로 관리 |
| `sellers` | 상품과 one-to-one unique | Product cascade로 관리 |
| `search_sessions` | sessionId unique, userId index | 소유자·페이지·상세 조회 |
| `search_conditions` | 세션·조건 관계 | 세션 조건 저장/복원 |
| `search_messages` | messageId unique | 마지막/시간순 메시지 조회 |
| `search_results` | `(session,product)`, `(session,rank)` unique | AI 결과 저장 |
| `carbon_quests` | `(user_id, quest_date)` unique | 당일 미션 조회/저장 |
| `product_view_records` | `(user_id,product_id,viewed_date)` unique | 중복 조회 방지 |
| `platform_redirect_histories` | user/time index, product FK | 누적 이동 수/이력 저장 |

`SearchSession`, `CarbonQuest`, `ProductViewRecord`, `PlatformRedirectHistory`, `RefreshToken`의 사용자 연결은 `User` 연관관계가 아니라 scalar `userId`다. 결합도를 낮추지만 FK 무결성, 탈퇴 cascade, 고아 데이터 처리를 애플리케이션이 책임져야 한다.

## 9. 외부 시스템 연동

### Kakao Local API

- 실제 `RestClient` 연동이 구현되어 있다.
- base URL 기본값: `https://dapi.kakao.com`
- 필수 설정: `KAKAO_REST_API_KEY`
- 용도: 사용자 지역 후보 검색, 상품 주소 좌표화.

### AI 검색 서버

- `AiSearchClient` 인터페이스와 확정된 `AiSearchRequest/AiSearchResponse/AiProductResponse` 계약이 존재한다.
- 실제 HTTP 클라이언트 구현, base URL, timeout/retry/circuit breaker는 없다.
- 현재 `UnconfiguredAiSearchClient`가 502를 발생시킨다.

### 외부 중고 플랫폼

- 백엔드가 직접 크롤링/검색하지 않는다. AI 결과의 상품 URL을 저장하고 redirect 시 허용 도메인만 검증한다.

## 10. 인증·보안

- Stateless Spring Security이며 CSRF는 비활성화되어 있다.
- access token은 `Authorization: Bearer` 헤더로 받고 `JwtAuthenticationFilter`가 인증 principal을 구성한다.
- 토큰은 `TokenProvider`가 HMAC-SHA256으로 직접 생성·검증한다.
- refresh token은 HttpOnly cookie와 DB에 함께 저장된다.
- 비밀번호는 BCrypt를 사용한다.
- CORS 허용 origin은 설정값으로 관리하며 기본 로컬 프론트 주소가 포함된다.
- 역할/권한 모델은 없고 인증 여부만 검사한다.

보완점:

1. 기본 JWT secret을 운영에서 절대 사용하지 않도록 startup validation이 필요하다.
2. 자체 JWT 파싱은 제한된 claim 형식에 강하게 결합되어 있으므로 검증된 JWT 라이브러리 도입을 검토한다.
3. refresh token 원문을 DB에 저장하므로 유출 피해를 줄이려면 hash 저장을 검토한다.
4. 로그인 rate limit, 계정 잠금, 보안 이벤트 감사가 없다.
5. production cookie의 `Secure`, SameSite, CORS 조합을 배포 환경에서 검증해야 한다.

## 11. 예외 처리

`GlobalExceptionHandler`가 다음을 공통 `ApiResponse.error`로 변환한다.

| 예외 | HTTP |
| --- | ---: |
| `UnauthorizedException` | 401 |
| `IllegalArgumentException`, validation/type mismatch, unreadable body | 400 |
| `KakaoLocalException` | 예외가 가진 status |
| `ExternalPlatformSearchException`, `AiServerUnavailableException` | 502 |
| `ProductNotFoundException`, `SearchSessionNotFoundException`, `RedirectTargetNotFoundException` | 404 |
| `RedirectForbiddenException` | 403 |

미처리 런타임/DB 예외를 위한 명시적 500 handler, 로깅 correlation ID, 안정된 error code 필드는 없다. 현재는 상세 exception message가 그대로 노출될 수 있어 운영 응답과 로그를 분리할 필요가 있다.

## 12. 구현 상태

### ✅ 구현 완료·사용 가능

- 회원가입/로그인/토큰 갱신/로그아웃/비밀번호 변경
- 내 프로필 조회·수정 및 위치 저장
- Kakao 지역 검색·좌표 변환(키 설정 시)
- 마이페이지 dashboard 통계
- 탄소 미션 조회 및 상품 조회 중복 집계
- 플랫폼 redirect 검증·기록
- 최근 검색 세션 목록 및 세션 상세 조회(저장 데이터가 있을 때)
- 상품/이미지/판매자 및 검색/활동 JPA 모델

### ⚠️ 부분 구현·임시 구현

- Best Deal: AI가 아닌 임시 점수, 메모리 정렬/페이징, favorite 고정값
- 거리 정렬: 비정형 JSON 계약
- 로컬 초기 데이터: 실제 MySQL에 저장되지만 external ID/URL이 `mock_*`
- 검색 세션 DTO: 메시지 발신 주체 등 UI 복원에 필요한 메타데이터 부족 가능
- 회원 탈퇴: scalar userId 기반 연관 데이터 정리 정책 부재
- Redis: 의존성만 있고 사용하지 않음

### ❌ 미구현·실사용 불가

- 실제 AI 서버 HTTP 연동
- `POST /search-sessions/{sessionId}/messages` 추가 질문
- `GET /search-sessions/{sessionId}/results` 검색 결과 목록
- `GET /products/{productId}` 상품 상세
- `GET /products/{productId}/similar` 유사 매물
- `POST /products/{productId}/refresh` 원본 갱신
- favorite 추가/삭제/목록
- redirect history 목록
- controller/security/API 통합 테스트와 운영용 DB migration

## 13. 부족한 기능 및 개선 과제

### [P0] AI 검색 생성 경로 실사용 불가

- 현상: `POST /search-sessions`가 항상 502를 반환한다.
- 원인: `UnconfiguredAiSearchClient`만 Bean으로 등록되어 있다.
- 영향: 핵심 검색 세션/상품/검색 결과 데이터가 정상 사용자 흐름에서 생성되지 않는다.
- 조치: AI 팀 계약 확정 후 HTTP client, DTO adapter, timeout, 오류 매핑, 계약 테스트를 구현한다.
- 완료 기준: AI 응답을 받아 transaction으로 세션·상품·결과가 저장되고 201 응답 및 실패 시나리오 테스트가 통과한다.

### [P0] 마이페이지 최근 검색의 정상 데이터 생성 의존성

- 현상: 최근 검색 조회 API는 있으나 AI 검색 생성이 막혀 fixture 외 데이터가 생기지 않는다.
- 영향: 마이페이지의 핵심 목록이 빈 상태에 머문다.
- 조치: AI 연동 전에는 명시적인 개발 stub profile을 제공하거나 계약 확정 후 생성 API를 먼저 완성한다.
- 완료 기준: 로그인 사용자 검색 → 마이페이지 재진입 시 같은 세션이 표시된다.

### [P1] 회원 탈퇴 데이터 정리 정책

- 현상: 검색/활동/refresh 데이터가 scalar userId를 사용하고 FK cascade가 없다.
- 영향: 고아 데이터 및 개인정보 보존 문제가 발생할 수 있다.
- 조치: 삭제/익명화/법적 보존 정책을 정하고 하나의 탈퇴 transaction에서 반영한다.
- 완료 기준: 탈퇴 통합 테스트로 관련 테이블 상태를 검증한다.

### [P1] Best Deal 운영화

- 현상: 임시 점수와 전체 메모리 페이징을 사용하며 favorite가 고정값이다.
- 영향: 추천 신뢰성, 응답 성능, 사용자별 표시 정확성이 떨어진다.
- 조치: 추천 점수 source를 확정하고 DB pageable query 또는 snapshot을 도입하며 favorite를 사용자 기준으로 계산한다.
- 완료 기준: 대량 데이터 성능 테스트와 정렬/카테고리/즐겨찾기 계약 테스트를 통과한다.

### [P1] API 계약과 프론트 기능의 공백

- 현상: 검색 결과, 추가 질문, 상품 상세 API가 없다.
- 영향: 검색 결과 화면을 실제 DB/API로 완전히 전환할 수 없다.
- 조치: 우선순위대로 results → product detail → messages API를 구현한다.
- 완료 기준: MSW 없이 검색 결과·상세·추가 질문 흐름이 동작한다.

### [P1] DB migration 부재

- 현상: `ddl-auto=update`에 의존한다.
- 영향: 환경별 스키마 차이와 배포 rollback 위험이 있다.
- 조치: Flyway/Liquibase migration과 schema 검증 모드를 도입한다.
- 완료 기준: 빈 DB와 기존 DB 양쪽에서 재현 가능한 migration 테스트가 통과한다.

### [P2] API 오류 계약 강화

- 현상: error code와 fallback 500 handler가 없고 내부 메시지 노출 가능성이 있다.
- 조치: 안정된 error code, 사용자 메시지, 내부 로그를 분리하고 trace ID를 추가한다.

### [P2] 인증 강화

- 현상: 자체 JWT 파서, 원문 refresh token 저장, rate limit 부재.
- 조치: 표준 라이브러리, refresh hash/rotation/reuse 탐지, 로그인 보호를 적용한다.

### [P2] 테스트 공백

- 현상: 도메인/서비스/Repository 테스트는 있으나 Controller, Security, 실제 MySQL, 외부 API contract 테스트가 부족하다.
- 조치: MockMvc 및 Testcontainers 기반 통합 테스트를 추가한다.

### [P3] 구조 정리

- 빈 `* 2` 디렉터리 제거, 사용하지 않는 Redis 의존성 결정, 임시 DTO/주석의 추적 issue 연결이 필요하다.

## 14. 핵심 흐름

### 로그인

```text
Client
  → AuthController.login
  → AuthService.login
  → UserRepository + BCrypt 검증
  → TokenProvider(access/refresh)
  → RefreshTokenRepository 저장
  → access token body + refresh cookie
```

### AI 검색 생성(의도된 흐름)

```text
Client
  → SearchSessionController.create
  → SearchSessionService
  → AiSearchClient ── 현재 Unconfigured 구현으로 502
  → ProductUpsertService
  → SearchSession/Condition/Message/Result Repository
  → 201 Created
```

### 마이페이지

```text
Profile Page
  ├→ GET /users/me
  │    → UserService → users
  ├→ GET /users/me/dashboard
  │    → UserDashboardService
  │       ├→ redirect history count
  │       ├→ completed search count
  │       └→ today's carbon quest
  └→ GET /users/me/search-sessions
       → SearchSessionService → sessions + conditions + messages
```

### 상품 조회 미션

```text
Product detail
  → POST /products/{id}/views
  → user row pessimistic lock
  → product/eligibility 확인
  → 당일 unique 조회 확인
  → ProductViewRecord 저장
  → CarbonQuest 증가 및 3회 시 보상 완료
```

### 외부 플랫폼 이동

```text
Client
  → POST /products/{id}/redirect
  → Product 조회
  → HTTPS + 플랫폼 허용 도메인 검증
  → PlatformRedirectHistory 저장
  → redirectUrl 반환
```

## 15. 결론 및 권장 우선순위

현재 백엔드는 인증·회원·지역·상품 활동·마이페이지 조회 기반이 구축되어 있고 MySQL에서 실제 데이터를 읽고 쓸 수 있다. 다만 핵심 데이터 생성원인 AI 검색 연동이 비활성 상태이며, 검색 결과/상품 상세 API가 없어 프론트의 주요 화면을 mock 없이 완성하기에는 공백이 있다.

권장 작업 순서는 다음과 같다.

1. **P0 — AI 계약 확정 및 `AiSearchClient` 실제 구현**
2. **P0 — 검색 생성부터 마이페이지 최근 검색까지 end-to-end 검증**
3. **P1 — 검색 결과 조회와 상품 상세 API 구현**
4. **P1 — Best Deal 임시 로직과 메모리 페이징 운영화**
5. **P1 — 회원 탈퇴 데이터 정책 및 DB migration 도입**
6. **P2 — Security/API/MySQL 통합 테스트와 오류 계약 강화**

마이페이지를 가장 빠르게 완성한다는 목표만 보면 `GET /users/me`, `GET /users/me/dashboard`, `GET /users/me/search-sessions`는 이미 존재한다. 따라서 최우선 병목은 화면 조회 API 자체가 아니라 **정상적인 검색 세션 데이터 생성 경로와 프론트 mock 해제 후 계약 일치 검증**이다.
