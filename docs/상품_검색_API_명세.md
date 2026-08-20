# 상품 검색 API — 백엔드가 만들어야 할 것

AI의 `search` 노드가 상품 후보를 가져올 곳이다. 지금은 없어서 **검색 결과가 항상 0건**이다.

만들 것은 둘이다.

1. 크롤러 결과를 DB에 넣는 경로
2. AI가 부를 검색 API

---

## 왜 지금 0건인가

AI 쪽 `tools.py`가 이렇게 되어 있다.

```python
def search_products(query_parsed: dict, limit: int = 40) -> dict:
    """TODO: 백엔드 검색 API 연동 전까지 항상 ok:false."""
    return SearchResult(ok=False, error="search_products 미구현").model_dump()
```

이 함수가 아래 API를 부르게 되면 파이프라인이 끝까지 이어진다.

```
프론트 → 백엔드 → AI(parse_query) → ★ 이 API ★ → AI(validate·rerank) → 백엔드 → 프론트
```

---

## 1. 검색 API

### 요청

```http
POST /internal/products/search
Content-Type: application/json

{
  "product": "닌텐도 스위치 OLED",
  "budget": 300000,
  "used_allowed": true,
  "limit": 40
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `product` | string | 검색어. AI의 `query_parsed.product` 그대로 |
| `budget` | int \| null | 예산 상한(원). 없으면 `null` |
| `used_allowed` | bool | `false`면 새상품만 |
| `limit` | int | 반환할 최대 개수. 기본 40 |

경로는 바꿔도 된다. 정해지면 알려주면 AI 쪽 상수를 맞춘다.

### 응답

```http
200 OK

{
  "ok": true,
  "items": [ /* 통합 스키마 그대로 */ ],
  "error": null
}
```

실패할 때도 200으로 주고 `ok`로 구분한다. AI가 검색 실패를 정상 흐름으로 다룬다.

```json
{ "ok": false, "items": [], "error": "..." }
```

---

## 2. 지켜야 할 규칙 셋

### `budget`을 하드 필터로 쓰지 말 것

예산이 30만원인데 결과가 전부 31만원이면 **0건보다 근소한 초과를 보여주는 편이 낫다.**
"조금만 더 보태면 이게 있습니다"가 가능한 답이기 때문이다.

상한을 조금 넘겨 가져오고 최종 판단은 AI의 재랭킹에 맡긴다.

### `used_allowed: true`면 새상품도 함께 준다

중고만 주면 안 된다. **새것과 중고를 가로질러 비교하는 것이 이 서비스의 전제**라,
새상품이 빠지면 "정말 새것을 살 필요가 있는가"를 판단할 수 없다.

`false`일 때만 `ELEVENST`로 좁힌다.

### `limit` 기본값이 40인 이유

AI의 가격 점수는 **받은 후보 안에서의 상대 순위**다. 후보가 너무 적으면 최저가와
최고가가 붙어 점수가 의미를 잃는다. 플랫폼 넷을 합쳐 40건이 기준이다.

---

## 3. `items`는 통합 스키마 그대로

**필드를 줄이거나 이름을 바꾸지 않는다.** AI가 그 형태를 전제로 읽는다.

정의 원본은 `data-analysis/docs/통합_스키마_정의.md`이며, 실제 한 건은 이렇게 생겼다.

```jsonc
{
  "platform": "BUNJANG",
  "platform_product_id": "423774878",
  "url": "https://m.bunjang.co.kr/products/423774878",
  "title": "닌텐도 스위치 OLED 스플래툰 에디션( 상급) + 파우치가방",
  "price": 268000,
  "currency": "KRW",
  "description": "작동이상 없고 … 일반택배 5,000원 별도입니다",
  "images": ["https://media.bunjang.co.kr/product/423774878_1_….jpg", "…"],
  "condition_level": "LIGHTLY_USED",
  "condition_raw": "LIGHTLY_USED",
  "trade_method": ["PARCEL", "MEET"],
  "delivery_fee": {
    "status": "AVAILABLE",
    "payer": "BUYER",
    "min_fee": 5000,
    "home_delivery_fee": 5000,
    "options": [ /* 배송 수단별 상세 */ ],
    "raw": { /* 플랫폼 원본 */ }
  },
  "location": {
    "name": "경기도 고양시 덕양구 화정1동",
    "full_address": "경기도 고양시 덕양구 화정1동",
    "precision": "FULL",
    "regions": [ { "name": "…", "full_address": "…", "code": null } ],
    "coordinates": null
  },
  "price_range": null,
  "collected_at": "2026-08-20T…"
}
```

### 값이 정해져 있는 필드

| 필드 | 값 |
| --- | --- |
| `platform` | `BUNJANG` · `JOONGNA` · `NAVER_FLEAMARKET` · `ELEVENST` |
| `condition_level` | `NEW` · `LIKE_NEW` · `LIGHTLY_USED` · `USED` · `UNSPECIFIED` · `UNKNOWN` |
| `trade_method` | `PARCEL` · `MEET` 조합 |
| `delivery_fee.status` | `AVAILABLE` · `NOT_AVAILABLE` |
| `location.precision` | `FULL` · `DONG_ONLY` · `NONE` |

`Platform`·`ProductCondition` enum과 이미 같은 값이다.

---

## 4. 크롤러 데이터를 DB에 넣기

크롤러가 JSON 파일로 결과를 낸다.

```bash
cd data-analysis/crawler/unified
python3 run_unified_crawl.py "닌텐도 스위치 OLED" --limit 10 --validate
# → output/unified_닌텐도_스위치_OLED.json
```

파일 구조는 이렇다.

```jsonc
{
  "query": "닌텐도 스위치 OLED",
  "generatedAt": "2026-08-20T…",
  "sourceCounts": { "BUNJANG": 6, "JOONGNA": 6, "NAVER_FLEAMARKET": 6, "ELEVENST": 3 },
  "count": 21,
  "items": [ /* 위 형태 */ ]
}
```

`ProductUpsertService.upsert()`가 이미 있으니 그걸 재사용하면 된다.
`platform` + `platform_product_id`로 중복을 판단한다.

### 적재 방식은 정하기 나름이다

배치 스크립트든 관리자 엔드포인트든 상관없다. 해커톤 범위라면
**미리 몇 개 검색어를 돌려 DB에 넣어 두는 것**으로 충분하다.

실시간 크롤링은 검색당 30초가 걸려 시연에 맞지 않는다.

---

## 5. 지금 담을 곳이 없는 값

`Product` 엔티티에 **배송비 필드가 없다.**

```java
private boolean directTradeAvailable;
private boolean shippingAvailable;
// 배송비 금액 필드 없음
```

**총 지불액 비교가 이 서비스의 핵심인데 배송비를 저장할 곳이 없다.**
`min_fee`와 `home_delivery_fee`가 버려진다.

두 값을 나눠 둔 이유가 있다. 편의점 택배(반값·알뜰)가 가장 싸지만 **주변에 그 편의점이
없는 사용자에게는 선택지가 아니다.** 최저가만으로 비교하면 그런 사용자에게 실제보다
싸 보인다.

```
min_fee              편의점 포함 최저 배송비
home_delivery_fee    편의점 없이 받을 수 있는 최저 배송비
```

`home_delivery_fee`가 `null`이면 **편의점 픽업 외에 방법이 없다는 뜻**이다.

> 실측 140건 중 편의점이 더 싼 상품이 있었고 차액은 최대 3,000원이다. 저가 상품에서는
> 순위를 뒤집는다. 자세한 내용은 `data-analysis/docs/배송비_스코어링_규칙_검토.md`에 있다.

### 그 밖에 없는 것

| 통합 스키마 | 비고 |
| --- | --- |
| `condition_raw` | 플랫폼 원본 상태값. 디버깅용이라 없어도 된다 |
| `price_range` | 11번가 옵션 가격 범위. **표시가가 확정가가 아닐 수 있다** |
| `collected_at` | 수집 시각 |

`price_range`가 있는 상품은 실제 결제액이 표시가와 다르다. 화면에 단일 금액으로만
보이면 사용자가 결제 단계에서 다른 값을 본다.

---

## 6. 되는지 확인하는 법

API를 만든 뒤 직접 쳐 본다.

```bash
curl -X POST http://localhost:8080/internal/products/search \
  -H "Content-Type: application/json" \
  -d '{"product":"닌텐도 스위치 OLED","budget":300000,"used_allowed":true,"limit":40}'
```

| 확인 | |
| --- | --- |
| `ok: true`이고 `items`가 비어 있지 않은가 | DB 적재까지 됐다는 뜻 |
| `items[0].platform`이 네 값 중 하나인가 | enum이 맞다는 뜻 |
| `delivery_fee`가 통째로 들어 있는가 | AI가 총 지불액을 계산할 수 있다는 뜻 |
| `used_allowed: false`로 바꾸면 `ELEVENST`만 오는가 | 필터가 맞다는 뜻 |

그다음 AI 담당자에게 **경로와 포트**를 알려주면 `search_products` 스텁을 교체한다.

---

## 참고

| 문서 | 무엇 |
| --- | --- |
| `data-analysis/docs/통합_스키마_정의.md` | 상품 필드 전체. **데이터 SSOT** |
| `ai/docs/백엔드_연동_계약.md` 6장 | 이 API의 원본 계약 |
| `data-analysis/docs/배송비_스코어링_규칙_검토.md` | 배송비를 왜 나눠 두는지 |
