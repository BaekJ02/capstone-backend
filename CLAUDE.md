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

## Key Design Notes

- `StockSearchService` searches across KOSPI, KOSDAQ, ETF, NASDAQ, and NYSE using CSV data parsed with OpenCSV.
- Initial user balance is 10,000,000 KRW, set at signup in `UserService`.
- `TradeService` enforces balance/holdings checks before buy/sell and records every trade in the `Order` entity.
- The in-memory STOMP broker is not production-grade — a migration to RabbitMQ/Redis is on the roadmap.
