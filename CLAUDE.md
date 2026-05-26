# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (port 8080)
./gradlew bootRun

# Build
./gradlew build

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

## Architecture

Spring Boot backend for a stock trading simulation platform. No frontend code lives here — the frontend (Vite on port 5173) is a separate repo.

**Layer structure:** Controller → Service → Repository → JPA Entity

```
src/main/java/capstone/
├── config/          # Security (SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter),
│                    # WebSocket (WebSocketConfig), beans (AppConfig), CORS (CorsConfig - 빈 껍데기)
├── controller/      # REST + WebSocket message handlers
├── service/         # Business logic + KIS API calls
├── repository/      # Spring Data JPA interfaces
├── domain/          # JPA entities: User, Holding, Order, Watchlist
└── dto/             # Request/response objects
```

### External API: Korea Investment & Securities (KIS)

`KisAuthService` manages OAuth tokens. `StockService` wraps all KIS REST calls (domestic + overseas prices, charts, minute/yearly data). API credentials go in `application-secret.properties` (git-ignored):

```properties
kis.api.key=YOUR_KEY
kis.api.secret=YOUR_SECRET
jwt.secret=YOUR_JWT_SECRET
fmp.api.key=YOUR_FMP_KEY
koreaexim.api.key=YOUR_EXIM_KEY
claude.api.key=YOUR_CLAUDE_KEY
```

**FMP (Financial Modeling Prep)** — 미국주식 재무데이터 + 순위:
- `GET /stable/profile?symbol={symbol}&apikey={key}` → 시가총액(`marketCap`), 베타(`beta`), 52주 범위(`range`: `"low-high"` split), 배당금(`dividendYield` → fallback `lastDividend`), 거래량(`volume`)
- `GET /stable/ratios-ttm?symbol={symbol}&apikey={key}` → PER(`priceToEarningsRatioTTM`), PBR(`priceToBookRatioTTM`)
- PER, PBR은 `formatRatio()`로 소수점 2자리 반올림. null·빈값·파싱 불가 시 빈 문자열 반환
- `GET /stable/biggest-gainers?apikey={key}` → 미국 급상승 순위
- `GET /stable/biggest-losers?apikey={key}` → 미국 급하락 순위
- `GET /stable/most-actives?apikey={key}` → 미국 거래량 순위

### Authentication

JWT (Bearer 토큰) 방식. 세션 방식은 제거됨.

- 로그인 성공 응답: `{ "token": "...", "name": "..." }`
- 보호된 API 요청 시 헤더 필수: `Authorization: Bearer {token}`
- `JwtTokenProvider` → 토큰 생성/파싱/검증 (HS256, 24시간 만료)
- `JwtAuthenticationFilter` → `OncePerRequestFilter`, SecurityContextHolder에 userId(Long) 설정
- `SecurityConfig` → `/api/users/signup`, `/api/users/login`, `/ws/**`, `/api/stocks/**`, `/api/exchange/**`, `/api/market/**`, `/h2-console/**` 인증 없이 허용; 나머지 `/api/**` 인증 필요
- 컨트롤러에서 userId 추출: `(Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal()`
- 비밀번호: `BCryptPasswordEncoder`로 암호화 저장, `matches()`로 검증

### WebSocket / Real-time Data

STOMP over SockJS. 클라이언트는 아래 엔드포인트로 구독 요청을 보내고, 서버는 `/topic/domestic/{symbol}` 또는 `/topic/overseas/{symbol}`로 가격 업데이트를 브로드캐스트한다.

| 엔드포인트 | 용도 | KIS 슬롯 |
|---|---|---|
| `/app/subscribe/domestic/price` | 홈화면 현재가 전용 (H0STCNT0만) | 1개 |
| `/app/subscribe/domestic` | 종목 상세 (H0STCNT0 + H0STASP0) | 2개 |
| `/app/subscribe/overseas` | 미국주식 현재가/체결 (HDFSCNT0) | 1개 |
| `/app/subscribe/overseas/orderbook` | 미국주식 호가창 (HDFSASP0, 매수/매도 각 10호가) | 1개 |

KIS WebSocket 동시 구독 제한: **41개 슬롯**. 홈화면은 `/price` 엔드포인트로 슬롯 절약.

`StockSubscriptionService`가 활성 구독 목록을 관리하고 `KisWebSocketClient`를 직접 호출한다. `StockWebSocketService`는 브로드캐스트를 담당한다.

### Database

- **Development/Production:** MySQL (`jdbc:mysql://localhost:3306/capstone`), DDL auto-update

### CORS

`SecurityConfig`에서 모든 origin 허용 (`allowedOriginPatterns("*")`)으로 설정. CSRF 비활성화, 세션 STATELESS. `CorsConfig`는 빈 껍데기로 유지 (충돌 방지). WebSocket(`WebSocketConfig`)은 별도로 `setAllowedOriginPatterns("*")` 설정.

## Implemented Features

- 회원가입 / 로그인 / 로그아웃 (JWT 기반, BCrypt 비밀번호 암호화)
- 모의 매수 / 매도 / 보유종목 / 잔고 / 주문내역
- 종목 검색 (KOSPI / KOSDAQ / ETF / 미국주식)
- 현재가 / 차트 / 분봉 / 연봉 (국내 + 미국)
- WebSocket 실시간 주가 스트리밍
- 관심종목 추가/삭제/조회 (Watchlist 도메인, `/api/watchlist`)
- 종목별 상세정보 조회 (`/api/stocks/detail/domestic/{symbol}`, `/api/stocks/detail/overseas/{symbol}`)
  - 미국주식 상세정보는 KIS API 미제공으로 재무데이터(PER, EPS, 시가총액 등) 없음, 추후 Yahoo Finance 연동 예정
- KIS 웹소켓 실시간 연동 (`ws://ops.koreainvestment.com:21000`)
  - 국내주식 정규장(09:00~15:30) → `KisWebSocketClient`로 실시간 수신 후 `/topic/domestic/{symbol}` 브로드캐스트
  - 시간외/야간/주말 → 종가 고정 (`getDomesticStockPrice`, 3초 폴링)
  - 미국주식 정규장 → KIS 웹소켓 실시간 수신 (tr_id: HDFSCNT0, tr_key: D+거래소3자리+심볼, 예: DNASAAPL)
  - 미국주식 장외 → REST API 3초 폴링
  - 새 파일: `MarketTimeService.java` (시간대 판별), `KisWebSocketClient.java` (KIS WS 연결/구독/파싱)
  - `KisWebSocketClient` 구독 메서드:
    - `subscribe(symbol)`: H0STCNT0 + H0STASP0 동시 구독 (종목 상세용, 슬롯 2개)
    - `subscribePriceOnly(symbol)`: H0STCNT0만 구독 (홈화면용, 슬롯 1개)
    - `subscribeOverseas(symbol, exchange)`: HDFSCNT0 구독 (미국주식 현재가/체결)
    - `subscribeOverseasOrderbook(symbol, exchange)`: HDFSASP0 구독 (미국주식 호가창, tr_key: D+거래소+심볼)
    - `unsubscribe / unsubscribePriceOnly / unsubscribeOverseas / unsubscribeOverseasOrderbook`: 각각 대응하는 구독 취소
    - `handleOverseasOrderBook`: 매수/매도 각 10호가 파싱, 응답 토픽: `/topic/orderbook/{symbol}`
- `StockSubscriptionService`: `KisWebSocketClient` 의존성 주입. `subscribeDomesticPriceOnly(symbol)` — 중복 구독 방지(Set.add 반환값 체크) 후 `kisWebSocketClient.subscribePriceOnly()` 호출. `subscribeOverseasOrderbook / unsubscribeOverseasOrderbook` 추가
- `StockSubscriptionController` 엔드포인트:
  - `/subscribe/domestic` / `/unsubscribe/domestic`: 종목 상세용 (H0STCNT0 + H0STASP0)
  - `/subscribe/domestic/price` / `/unsubscribe/domestic/price`: 홈화면용 (H0STCNT0만)
  - `/subscribe/overseas` / `/unsubscribe/overseas`: 미국주식 현재가/체결 (HDFSCNT0)
  - `/subscribe/overseas/orderbook` / `/unsubscribe/overseas/orderbook`: 미국주식 호가창 (HDFSASP0)
  - `@Slf4j` 적용, 각 메서드 첫 줄에 `log.info` 추가
- 호가창: `OrderBookDto`, `GET /api/stocks/orderbook/domestic/{symbol}` (tr_id: FHKST01010200)
  - 국내 실시간 호가 웹소켓: H0STASP0 → `/topic/orderbook/{symbol}`
  - 국내 실시간 체결 웹소켓: H0STCNT0 → `/topic/tradetick/{symbol}` (TradeTickDto)
  - 미국 실시간 호가 웹소켓: HDFSASP0 → `/topic/orderbook/{symbol}` (국내와 동일 토픽 구조)
  - 미국 실시간 체결 웹소켓: HDFSCNT0 → `/topic/tradetick/overseas/{symbol}` (TradeTickDto, ASVL vs BIVL 비교로 매수/매도 구분)
  - 종목 상세 구독(`/subscribe/domestic`) 시 H0STCNT0 + H0STASP0 동시 구독
  - 체결 필드: 1건 = 46개 필드, `fields[offset+21]` 매수매도구분 (1=매수, 5=매도), offset = (fields.length/46 - 1) * 46
  - 호가 필드: 1건 = 62개 필드, offset = (fields.length/62 - 1) * 62
- 매매 수량 유효성 검사: 프론트(trade 함수) + 백엔드(TradeService.buy/sell) 모두 quantity < 1 차단
- 보유종목 실시간 DOM 업데이트: WebSocket 수신 시 `loadHoldings()` 대신 `updateHoldingRow(symbol)`으로 해당 행만 갱신
- Spring Security + JWT: `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig` 구현. 세션 방식 완전 제거, Stateless 인증
- 실현손익 조회: `Order.avgPrice` 저장, `GET /api/trade/profit?period=` (DAY/WEEK/MONTH/YEAR/ALL), `ProfitDto`, `ProfitItemDto`
- 환율: `ExchangeRateService` (한국수출입은행 API `https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON`, 주말/공휴일 최대 5일 이전 재시도, 1시간 캐싱 `@Scheduled`, SSL 검증 비활성화 적용)
- 환전 API: `ExchangeController` — `GET /api/exchange/rate`, `POST /api/exchange/krw-to-usd`, `POST /api/exchange/usd-to-krw`
- 달러 잔고: `User.dollarBalance`, 미국주식 매매 시 dollarBalance 차감/추가 (`TradeService.OVERSEAS_MARKETS`)
- 전체 계좌 현황: 원화 잔고 + 달러 잔고(KRW 환산) + 보유주식 평가금액 합산, `updateTotalAssets()` (async, /api/users/me + /api/exchange/rate 실시간 조회)
- 미국주식 금액 $ 표시: `isOverseas(market)` / `isOverseasSymbol(symbol)` 분기, `fmtMoney()` 헬퍼
- 미국주식 검색 개선: 대소문자 무관 검색 (`toUpperCase()` 비교), 종목명 정제 (`cleanName()` — " - " 이후·Inc./Corp./Ltd. 등 접미사 제거), 결과 표시 "Apple (AAPL)" 형태
- JPA 로그 정리: `spring.jpa.show-sql=false`, `spring.jpa.open-in-view=false`
- AI 챗봇: `AiController`, `AiService` 구현
  - POST /api/ai/chat (로그인 필요)
  - 멀티턴 대화 지원 (history 파라미터)
  - 모델: claude-haiku-4-5-20251001
  - 시스템 프롬프트: 주식 투자 전문 어시스턴트, 참고용 답변만 제공
  - WebClient로 Anthropic API 호출 (https://api.anthropic.com/v1/messages)
- AI 포트폴리오 분석: `AiController.buildHoldingsText()` — StockService로 실시간 현재가 조회 후 수익률 계산
  - POST /api/ai/analyze/holdings → 종목별 개별 분석
  - POST /api/ai/analyze/portfolio → 포트폴리오 전체 분석
  - POST /api/ai/analyze/recommend → 섹터/종목 추천
  - 국내주식: getDomesticStockPrice(), 미국주식: getOverseasStockPrice() 호출
  - 현재가 조회 실패 시 avgPrice fallback 처리
  - max_tokens: 2048 (챗봇보다 길게 설정)
- 시장 순위: `MarketRankingController`, `MarketRankingService`, `RankingItemDto` 구현 (인증 불필요)
  - GET /api/market/domestic/ranking?type=RISE|FALL|VOLUME → 국내주식 순위
  - GET /api/market/overseas/ranking?type=RISE|FALL|VOLUME → 미국주식 순위
  - 국내 RISE/FALL: KIS `/uapi/domestic-stock/v1/ranking/fluctuation` (tr_id: FHPST01700000), 파라미터 소문자 `fid_xxx`
  - 국내 VOLUME: KIS `/uapi/domestic-stock/v1/quotations/volume-rank` (tr_id: FHPST01710000), 파라미터 대문자 `FID_XXX`, symbol 필드 `mksc_shrn_iscd`
    - `FID_BLNG_CLS_CODE=3` (거래금액순, 0=평균거래량)
    - volume 필드: `acml_tr_pbmn` (누적 거래 대금, VOLUME 타입만 적용 / RISE·FALL은 `acml_vol`)
  - 미국 RISE/FALL: FMP `/stable/biggest-gainers`, `/stable/biggest-losers`
  - 미국 VOLUME: KIS `/uapi/overseas-stock/v1/ranking/trade-pbmn` (tr_id: HHDFS76320010), NYS+NAS+AMS 3개 거래소 각각 호출 후 `tamt`(거래대금) 기준 통합 정렬, 상위 20개 반환
  - 백엔드 정렬: RISE=changePercent 내림차순, FALL=오름차순, VOLUME=acml_tr_pbmn(국내)/tamt(미국) 내림차순
  - UriComponentsBuilder.fromUriString()으로 파라미터 빌드 (빈 값 인코딩 문제 방지)
- 시장 지수 API: `MarketRankingController` (`GET /api/market/indices`, 인증 불필요), `IndexDto` 신규 생성 (code, name, price, change, changePercent)
  - 코스피(0001), 코스닥(1001): KIS `/uapi/domestic-stock/v1/quotations/inquire-index-price` (tr_id: FHPUP02100000)
  - S&P500(SPX), 나스닥(COMP): KIS `/uapi/overseas-price/v1/quotations/inquire-time-indexchartprice` (tr_id: FHKST03030200)
  - SecurityConfig permitAll에 `/api/market/indices` 포함 (`/api/market/**` 이미 허용)
- 국내/미국주식 시가총액 순위 API (MARKET_CAP 타입)
  - 국내: KIS `/uapi/domestic-stock/v1/ranking/market-cap` (tr_id: FHPST01740000), GET /api/market/domestic/ranking?type=MARKET_CAP, `stck_avls` 기준 정렬
  - 미국: KIS `/uapi/overseas-stock/v1/ranking/market-cap` (tr_id: HHDFS76350100), `CURR_GB=0` 필수, NYS+NAS+AMS 통합, `tomv` 기준 정렬, GET /api/market/overseas/ranking?type=MARKET_CAP
  - `RankingItemDto`에 `marketCap` 필드 추가
- AI 시장 뉴스: `MarketNewsService`, `MarketNewsController`, `MarketNewsDto` 신규 생성
  - GET /api/market/news (인증 불필요, 캐싱 응답)
  - KIS 해외뉴스종합 API (tr_id: HHPSTH60100C1, NATION_CD=US) 제목 10건 수집
  - NAS 거래대금 상위 20종목 + 뉴스 제목 → Claude Haiku 분석 → JSON 구조화
  - 응답 필드: updatedAt, headlines, positive(sector+reason+stocks), negative(sector+reason+stocks), summary
  - StockDto: symbol, name, changePercent 필드
  - SectorDto: sector, reason, stocks 필드
  - `@Scheduled` cron: 매일 08:00/14:00/16:00/20:00, 22:30, 05:00 갱신
  - `@PostConstruct` 제거 — 서버 시작 시 자동 호출 없음, 스케줄 시간에만 갱신
  - build.gradle: `jackson-databind` 명시적 추가
  - AppConfig: `ObjectMapper` 빈 추가
- `KisAuthService` Race Condition 수정: `getAccessToken`, `issueAccessToken`, `getApprovalKey`, `issueApprovalKey` 모두 `synchronized` 적용. Double-checked locking 패턴으로 토큰 중복 발급 방지

## 알려진 이슈

- KIS WebSocket 동시 구독 슬롯 제한 (41개):
  - 국내주식 종목 상세: H0STCNT0 + H0STASP0 = 종목당 2슬롯 소모
  - 홈화면: `/subscribe/domestic/price`로 H0STCNT0만 구독 → 종목당 1슬롯
  - 종목 상세 창은 추후 별도 엔드포인트로 분리 예정
- 국내주식 호가창 잔량 1/2 문제:
  - REST API(FHKST01010200), 웹소켓(H0STASP0) 모두 실제값의 약 1/2 반환
  - KIS 오픈API 한계로 추정, 코드 자체는 정상
  - `handleOrderBook`: 호가 1건=62필드, 매도호가[3~12], 매수호가[13~22], 매도잔량[23~32], 매수잔량[33~42]
- 정규장 마감 직전(15:27~15:30) KIS API 불안정 구간:
  - 동시호가 전환 구간으로 웹소켓 데이터가 일시적으로 끊기거나 NaN 표시될 수 있음
  - 코드 버그 아님, KIS API 자체 특성
- 미국주식 호가창(HDFSASP0)/체결(HDFSCNT0) 웹소켓 구현 완료, 미국 정규장 테스트 필요
- 장외 시간 REST 폴링 초당 거래건수 초과 에러 (미수정)
- 정규장 외 시간대 실시간 주가: KIS API 한계로 구현 불가 (3초 폴링 방식으로 대체 제공)

## Key Design Notes

- `StockSearchService` searches across KOSPI, KOSDAQ, ETF, NASDAQ, and NYSE using CSV data parsed with OpenCSV.
- Initial user balance is 10,000,000 KRW, set at signup in `UserService`.
- `TradeService` enforces balance/holdings checks before buy/sell and records every trade in the `Order` entity.
- The in-memory STOMP broker is not production-grade — a migration to RabbitMQ/Redis is on the roadmap.
