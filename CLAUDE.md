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

H2 console is available at `http://localhost:8080/h2-console` when running locally (JDBC URL: `jdbc:h2:file:./data/capstone`, no password).

## Architecture

Spring Boot backend for a stock trading simulation platform. No frontend code lives here — the frontend (Vite on port 5173) is a separate repo.

**Layer structure:** Controller → Service → Repository → JPA Entity

```
src/main/java/capstone/
├── config/          # CORS (CorsConfig), WebSocket (WebSocketConfig), beans (AppConfig)
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
```

### Authentication

Session-based (no Spring Security or JWT). On login, `userId` is stored in `HttpSession`. Protected endpoints retrieve it via `session.getAttribute("userId")` and return 401 if absent.

### WebSocket / Real-time Data

STOMP over SockJS. Clients send to `/app/subscribe/domestic` or `/app/subscribe/overseas`; the server publishes price updates to `/topic/domestic/{symbol}` or `/topic/overseas/{symbol}`. `StockSubscriptionService` manages active subscriptions; `StockWebSocketService` does the broadcasting.

### Database

- **Development:** H2 file-based at `./data/capstone` (DDL auto-update, no migration needed)
- **Production target:** MySQL (driver included, not yet wired up)

### CORS

`CorsConfig` explicitly allows `http://localhost:5173`, `http://localhost:3000`, and `https://*.ngrok-free.app` with credentials. Session cookies require `SameSite=None; Secure` (set in `application.properties`) for cross-origin requests via ngrok.

## Implemented Features

- 회원가입 / 로그인 / 로그아웃 (session-based)
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
- 호가창: `OrderBookDto`, `GET /api/stocks/orderbook/domestic/{symbol}` (tr_id: FHKST01010200)
  - 실시간 호가 웹소켓: H0STASP0 → `/topic/orderbook/{symbol}`
  - 실시간 체결 웹소켓: H0STCNT0 → `/topic/tradetick/{symbol}` (TradeTickDto)
  - 국내주식 구독 시 H0STCNT0 + H0STASP0 동시 구독
- 매매 수량 유효성 검사: 프론트(trade 함수) + 백엔드(TradeService.buy/sell) 모두 quantity < 1 차단
- 보유종목 실시간 DOM 업데이트: WebSocket 수신 시 `loadHoldings()` 대신 `updateHoldingRow(symbol)`으로 해당 행만 갱신

## Key Design Notes

- `StockSearchService` searches across KOSPI, KOSDAQ, ETF, NASDAQ, and NYSE using CSV data parsed with OpenCSV.
- Initial user balance is 10,000,000 KRW, set at signup in `UserService`.
- `TradeService` enforces balance/holdings checks before buy/sell and records every trade in the `Order` entity.
- The in-memory STOMP broker is not production-grade — a migration to RabbitMQ/Redis is on the roadmap.
