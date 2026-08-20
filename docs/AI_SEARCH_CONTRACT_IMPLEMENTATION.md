# AI 검색 공통 JSON 계약 백엔드 반영

## 1. 작업 배경

AI 서버가 자연어 조건 분석, 중고·신품 수집, 추천 점수 산정,
순위 배치, 추천 이유 생성을 담당하는 구조로 역할을 확정했다.

백엔드는 AI 서버가 반환한 결과를 다음 용도로 사용한다.

- AI 응답 계약 검증
- 상품 원본 정보 upsert
- 배송비·배송 옵션 저장
- 11번가 시세 기준 스냅샷 저장
- AI 추천 점수·순위·근거·거리 저장
- 탄소 절감, 사용자 활동 등 백엔드 도메인 정보 결합

이 문서는 AI → Backend 공통 JSON이 확정된 시점부터 이를
백엔드 코드에 반영한 작업만을 다룬다. 프론트엔드는 수정하지 않았다.

---

## 2. 확정된 AI 응답 구조

최상위 AI 응답은 다음 정보를 포함한다.

```json
{
  "requestId": "req_01",
  "sessionId": "ss_01",
  "scoring": {
    "version": "v1"
  },
  "parsedConditions": {},
  "assistantMessage": "총 34개의 상품을 추천했어요.",
  "marketReference": {},
  "totalResultCount": 34,
  "products": []
}
```

### 검색 결과 구성

- 번개장터, 중고나라, N플리마켓 중고 상품
- 11번가 인기 신품 1개
- 11번가 신품도 `products` 배열에 포함
- AI가 산정한 점수에 따라 중고 상품 사이에 배치
- `totalResultCount`는 중고 상품과 11번가 신품을 모두 포함한 실제 개수
- 중고 상품은 플랫폼별 최대 12개, 전체 최대 36개
- 11번가 신품을 포함하면 전체 최대 37개

### 서비스 역할

| AI 서버 | 백엔드 |
| --- | --- |
| 자연어 검색 조건 분석 | 사용자 원문·세션·위치 맥락 전달 |
| 외부 플랫폼 상품 수집 | 응답 형식·필수값·중복 검증 |
| 추천 점수·순위·이유 생성 | AI 결과 저장 및 조회 |
| 11번가 중앙값 시세 산출 | 시세 스냅샷 저장 |
| 상품·배송 원본 정보 제공 | 상품 upsert 및 도메인 정보 결합 |

---

## 3. enum 반영

### 플랫폼

```java
BUNJANG
JOONGNA
NAVER_FLEAMARKET
ELEVENST
```

### 상품 상태

```java
NEW
LIKE_NEW
LIGHTLY_USED
USED
UNSPECIFIED
UNKNOWN
```

`GOOD`은 공통 계약에 포함되지 않는다. AI 서버는 반드시 위 6개 상태 중
하나로 정규화해야 한다.

### 거래 방식

```java
DIRECT
DELIVERY
```

### 배송 상태

```java
AVAILABLE
NOT_AVAILABLE
```

### 배송비 부담 주체

```java
SELLER
BUYER
```

판단할 수 없으면 임의의 `UNKNOWN`을 넣지 않고 `null`을 사용한다.

### 배송 수단

```java
FREE
STANDARD
CONVENIENCE_STORE
UNKNOWN
```

### 편의점 택배사

```java
GS25
CU
```

일반 배송이거나 택배사를 특정할 수 없으면 `null`을 사용한다.

---

## 4. AI 응답 DTO 변경

### 최상위 응답

`AiSearchResponse`를 다음 구조로 변경했다.

```java
String requestId
String sessionId
AiScoringResponse scoring
AiParsedConditionsResponse parsedConditions
String assistantMessage
AiMarketReferenceResponse marketReference
int totalResultCount
List<AiRecommendedProductResponse> products
```

기존 `resultCount`는 확정 계약에 맞춰 `totalResultCount`로 변경했다.

### 추천 항목

`AiRecommendedProductResponse`에 다음 필드를 추가했다.

```java
AiScoreBreakdownResponse scoreBreakdown
Double distanceKm
```

### 배송 DTO

다음 DTO를 추가했다.

- `AiDeliveryFeeResponse`
- `AiDeliveryExtraCostResponse`
- `AiDeliveryOptionResponse`

`extraCost`는 다음 구조를 사용한다.

```json
{
  "jejuFee": 6000,
  "remoteAreaFee": 6000,
  "description": "제주 및 도서산간 지역은 추가 배송비가 발생합니다."
}
```

`rawCode`는 플랫폼에 따라 문자열 또는 숫자가 올 수 있으므로
`String` 또는 enum으로 제한하지 않고 Jackson `JsonNode`로 받는다.

### 위치 호환

확정 JSON의 위치 구조를 기존 지오코딩 로직과 연결했다.

```json
{
  "displayName": "판교동",
  "latitude": 37.3947,
  "longitude": 127.1111
}
```

---

## 5. 상품·배송 저장 구조

### 연관관계

```text
Product
 └─ ProductDelivery (1:1)
     └─ ProductDeliveryOption (1:N)
```

### `product_deliveries`

| 필드 | 설명 |
| --- | --- |
| `status` | 배송 가능 여부 |
| `payer` | 배송비 부담 주체 |
| `min_fee` | 선택 가능한 배송 수단 중 최저 배송비 |
| `home_delivery_fee` | 편의점 픽업이 필요 없는 배송비 |
| `jeju_fee` | 제주 지역 추가 배송비 |
| `remote_area_fee` | 도서산간 추가 배송비 |
| `extra_cost_description` | 추가 배송비 안내 문구 |

### `product_delivery_options`

| 필드 | 설명 |
| --- | --- |
| `method` | 정규화된 배송 수단 |
| `carrier` | `GS25`, `CU` 또는 `null` |
| `requires_pickup_point` | 편의점 등 픽업 장소 방문 필요 여부 |
| `fee` | 해당 배송 수단의 비용 |
| `raw_code_json` | 플랫폼 원본 코드의 JSON 표현 |
| `display_order` | AI 응답의 배송 옵션 순서 |

### upsert 규칙

- `deliveryFee`가 있으면 배송 정보와 옵션을 저장한다.
- 이미 저장된 상품은 최신 AI 응답의 배송 정보로 교체한다.
- `deliveryFee: null`이면 기존 배송 정보를 제거한다.
- 11번가의 `{ "status": "AVAILABLE" }` 축약 형태를 허용한다.
- 상품이 삭제되면 배송 정보와 옵션도 cascade 삭제된다.

---

## 6. 검색 세션·추천 결과 저장

### `search_sessions`

`scoring_version`을 추가해 AI 점수 산정 버전을 저장한다.

### `search_market_references`

검색 세션별 11번가 시세 기준을 스냅샷으로 저장한다.

| 필드 | 설명 |
| --- | --- |
| `product_name` | 기준 상품명 |
| `source_platform` | `ELEVENST` |
| `source_name` | 시세 출처명 |
| `reference_type` | 현재 `POPULAR_NEW_PRODUCT` |
| `median_price` | 11번가 가격 표본 중앙값 |
| `sample_count` | 중앙값 산출에 사용한 표본 수 |
| `calculated_at` | 시세 계산 시각 |
| `source_url` | 기준 상품 URL |

### `search_results`

기존 순위·추천 점수·추천 이유에 다음 필드를 추가했다.

```text
price_score
quality_score
convenience_score
distance_km
```

11번가 및 위치를 확인할 수 없는 상품의 `convenienceScore`, `distanceKm`는
`null`을 허용한다.

---

## 7. nullable 정책

### `externalViewCount`

기존에는 AI가 `null`을 보내면 백엔드가 `0`으로 바꿔 저장했다.
이는 "수집하지 못함"과 "실제 조회수 0"을 구분하지 못하므로 다음과 같이
변경했다.

```text
products.external_view_count: nullable
Product.externalViewCount: Long
ProductDetailResponse.viewCount: Long
```

### 배송 정보

- `deliveryFee.status`는 `deliveryFee` 객체가 존재하면 필수다.
- `payer`, `minFee`, `homeDeliveryFee`, `extraCost`, `options`는 nullable이다.
- 추가 배송비를 확인하지 못하면 금액을 `0`으로 만들지 않는다.

---

## 8. AI 응답 검증

`SearchSessionService`에서 다음 계약을 검증한다.

- 요청과 응답의 `requestId` 일치
- 요청과 응답의 `sessionId` 일치
- `scoring.version` 필수
- `totalResultCount >= 0`
- `totalResultCount == products.size()`
- `rank`가 1부터 결과 개수까지 연속
- `products` 배열이 rank 오름차순
- `recommendationScore`가 0에서 100 사이
- 순위가 낮아질수록 점수가 높아지지 않음
- `platform + externalProductId` 중복 금지
- `ELEVENST` 인기 신품은 최대 1개

위반한 응답은 잘못된 AI 검색 응답으로 판단하고 저장하지 않는다.

---

## 9. 테스트

다음 범위를 검증했다.

- 확정 AI JSON 역직렬화
- 플랫폼·상태·거래·배송 enum 파싱
- 구조화된 `extraCost` 파싱
- 문자열 `rawCode` 파싱
- 배송 정보·옵션 서비스 변환
- 배송 정보·옵션 JPA 저장·조회·cascade 삭제
- 검색 세션의 점수 버전·시세 스냅샷 저장
- 검색 결과의 점수 구성·거리 저장
- 응답 개수와 배열 크기 정합성

전체 검증 결과:

```text
./gradlew test
BUILD SUCCESSFUL
```

---

## 10. DB 변경

### 추가 테이블

```text
product_deliveries
product_delivery_options
search_market_references
```

### 추가·변경 컬럼

```text
search_sessions.scoring_version

search_results.price_score
search_results.quality_score
search_results.convenience_score
search_results.distance_km

products.external_view_count nullable
```

현재 로컬 설정의 `spring.jpa.hibernate.ddl-auto` 값은 `update`이므로 서버 실행 시
로컬 MySQL 스키마가 갱신된다.

---

## 11. 남은 연동 작업

이번 작업은 AI 응답 계약, 검증, 도메인 변환, DB 저장까지를 반영했다.
다만 현재 `AiSearchClient` 구현체는 `UnconfiguredAiSearchClient`이므로 실제 AI 서버
HTTP 연동은 별도로 필요하다.

- AI 서버 base URL 환경변수 연결
- HTTP `AiSearchClient` 구현
- 연결·응답 타임아웃 설정
- AI 서버 오류를 502 응답으로 변환
- 실제 AI 서버를 사용한 통합 테스트

---

## 12. PR 체크리스트

- [x] 프론트엔드 미수정
- [x] AI 응답 DTO 확정 JSON 반영
- [x] enum 정합성 확인
- [x] 배송비·추가 배송비·배송 옵션 저장
- [x] 시세 스냅샷 저장
- [x] AI 점수 구성·거리 저장
- [x] AI 응답 검증 강화
- [x] nullable 정책 반영
- [x] JPA 저장·삭제 테스트
- [x] 전체 테스트 통과
- [ ] 실제 AI 서버 HTTP 연동
