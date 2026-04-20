# 캡스톤 백엔드 API 문서

프론트엔드 개발자를 위한 API 연동 가이드입니다.

---

## 서버 연결 방법

백엔드 개발자가 매번 작업 시작 전에 Spring 서버를 실행하고 ngrok 주소를 공유해줘요.

| 항목 | 값 |
|------|-----|
| 로컬 서버 포트 | `8080` |
| 외부 접속 주소 | 백엔드 개발자가 매번 공유 (ngrok 주소) |
| WebSocket 엔드포인트 | `{ngrok주소}/ws` |

> ⚠️ ngrok 주소는 서버를 재시작할 때마다 바뀌어요. 백엔드 개발자에게 새 주소를 받아서 사용해주세요.

---

## ngrok 경고 페이지 해결

ngrok 주소로 API를 호출할 때 **반드시 아래 헤더를 추가**해야 해요.

```javascript
headers: {
    'ngrok-skip-browser-warning': 'true'
}
```

---

## 필요한 라이브러리

WebSocket 연결을 위해 아래 패키지를 설치해주세요.

```bash
npm install sockjs-client @stomp/stompjs
```

---

## 공통 설정

JWT 토큰 기반 인증을 사용해요. 로그인 후 받은 토큰을 저장해두고, 보호된 API 요청 시 `Authorization` 헤더에 포함해야 해요.

```javascript
// 토큰 저장 (로그인 성공 후)
localStorage.setItem('token', response.data.token);

// API 인스턴스 생성
const api = axios.create({
    baseURL: NGROK_URL,
    headers: { 'ngrok-skip-browser-warning': 'true' }
});

// 요청 인터셉터로 토큰 자동 첨부
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});
```

---

## REST API 목록

### 👤 회원 API

#### 1. 회원가입

```
POST {ngrok주소}/api/users/signup
```

**요청 Body**
```json
{
    "email": "test@test.com",
    "password": "1234",
    "name": "홍길동"
}
```

**응답**
```
회원가입 성공! ID: 1
```

---

#### 2. 로그인

```
POST {ngrok주소}/api/users/login
```

**요청 Body**
```json
{
    "email": "test@test.com",
    "password": "1234"
}
```

**응답**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "name": "홍길동"
}
```

> 로그인 성공 시 `token`을 `localStorage`에 저장해두고, 이후 모든 보호된 API 요청 헤더에 `Authorization: Bearer {token}`을 첨부해야 해요.

---

#### 3. 로그아웃

```
POST {ngrok주소}/api/users/logout
```

---

#### 4. 내 정보 조회

```
GET {ngrok주소}/api/users/me
```

**응답**
```json
{
    "id": 1,
    "email": "test@test.com",
    "name": "홍길동",
    "balance": 10000000.0
}
```

---

### 💰 매매 API

> ⚠️ 모든 매매 API는 로그인 후에 사용 가능해요. 미로그인 시 401 응답이 와요.

#### 5. 매수

```
POST {ngrok주소}/api/trade/buy
```

**요청 Body**
```json
{
    "symbol": "005930",
    "name": "삼성전자",
    "market": "KOSPI",
    "type": "BUY",
    "quantity": 10,
    "price": 75000
}
```

**응답**
```
매수 완료
```

---

#### 6. 매도

```
POST {ngrok주소}/api/trade/sell
```

**요청 Body**
```json
{
    "symbol": "005930",
    "name": "삼성전자",
    "market": "KOSPI",
    "type": "SELL",
    "quantity": 5,
    "price": 80000
}
```

**응답**
```
매도 완료
```

---

#### 7. 잔고 조회

```
GET {ngrok주소}/api/trade/balance
```

**응답**
```json
9250000.0
```

---

#### 8. 보유 종목 조회

```
GET {ngrok주소}/api/trade/holdings
```

**응답**
```json
[
    {
        "id": 1,
        "symbol": "005930",
        "name": "삼성전자",
        "market": "KOSPI",
        "quantity": 10,
        "avgPrice": 75000.0
    }
]
```

---

#### 9. 주문 내역 조회

```
GET {ngrok주소}/api/trade/orders
```

**응답**
```json
[
    {
        "id": 1,
        "symbol": "005930",
        "name": "삼성전자",
        "type": "BUY",
        "quantity": 10,
        "price": 75000.0,
        "createdAt": "2026-04-15T23:54:17.729443"
    }
]
```

---

### 📈 주식 시세 API

#### 10. 국내 주식 현재가 조회

```
GET {ngrok주소}/api/stocks/domestic/{종목코드}
```

**응답**
```json
{
    "symbol": "005930",
    "price": "75000",
    "change": "1400",
    "changePercent": "1.90"
}
```

---

#### 11. 미국 주식 현재가 조회

```
GET {ngrok주소}/api/stocks/overseas/{종목코드}?exchange={거래소코드}
```

| 거래소 | 코드 | 예시 종목 |
|--------|------|-----------|
| 나스닥 | NAS | AAPL, TSLA, NVDA |
| 뉴욕 | NYS | BRK 등 |
| 아멕스 | AMS | VOO, SPY 등 |

---

#### 12. 종목 검색

```
GET {ngrok주소}/api/stocks/search?keyword={검색어}
```

국내 주식, ETF, 미국 주식 모두 검색 가능해요. 최대 20개 결과 반환해요.

**응답**
```json
[
    { "symbol": "005930", "name": "삼성전자", "market": "KOSPI" },
    { "symbol": "AAPL", "name": "Apple Inc. - Common Stock", "market": "NASDAQ" }
]
```

**market 값 종류**

| market | 설명 |
|--------|------|
| KOSPI | 코스피 |
| KOSDAQ | 코스닥 |
| ETF | 국내 ETF |
| NASDAQ | 나스닥 |
| NYSE | 뉴욕증권거래소 |
| AMEX | 아멕스 |
| OTHER | 기타 미국 거래소 |

---

#### 13. 국내 주식 차트 데이터

```
GET {ngrok주소}/api/stocks/chart/domestic/{종목코드}?period={기간}
```

| period | 의미 |
|--------|------|
| D | 일봉 (기본값) |
| W | 주봉 |
| M | 월봉 |
| Y | 연봉 |

**응답**
```json
[
    {
        "date": "20260415",
        "open": "75000",
        "high": "76000",
        "low": "74000",
        "close": "75500",
        "volume": "12345678"
    }
]
```

---

#### 14. 국내 주식 분봉 데이터

```
GET {ngrok주소}/api/stocks/chart/domestic/{종목코드}/minute?timeUnit={분}
```

| timeUnit | 의미 |
|----------|------|
| 1 | 1분봉 (기본값) |
| 3 | 3분봉 |
| 5 | 5분봉 |
| 10 | 10분봉 |
| 30 | 30분봉 |
| 60 | 60분봉 |

---

#### 15. 미국 주식 차트 데이터

```
GET {ngrok주소}/api/stocks/chart/overseas/{종목코드}?exchange={거래소}&period={기간}
```

| period | 의미 |
|--------|------|
| 0 | 일봉 (기본값) |
| 1 | 주봉 |
| 2 | 월봉 |

---

#### 16. 미국 주식 분봉 데이터

```
GET {ngrok주소}/api/stocks/chart/overseas/{종목코드}/minute?exchange={거래소}&timeUnit={분}
```

---

#### 17. 미국 주식 연봉 데이터

```
GET {ngrok주소}/api/stocks/chart/overseas/{종목코드}/yearly?exchange={거래소}
```

---

## WebSocket 실시간 데이터

### 연결 방법

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const NGROK_URL = 'https://xxxx.ngrok-free.dev';

const client = new Client({
    webSocketFactory: () => new SockJS(`${NGROK_URL}/ws`),
    onConnect: () => console.log('WebSocket 연결 성공!')
});

client.activate();
```

---

### 국내 주식 구독

```javascript
// 구독 시작
client.publish({ destination: '/app/subscribe/domestic', body: '005930' });

// 데이터 수신 (3초마다 업데이트)
client.subscribe('/topic/domestic/005930', (message) => {
    const data = JSON.parse(message.body);
    // {symbol: "005930", price: "75000", change: "1400", changePercent: "1.90"}
});

// 구독 취소
client.publish({ destination: '/app/unsubscribe/domestic', body: '005930' });
```

---

### 미국 주식 구독

```javascript
// 구독 시작 (종목코드,거래소코드 형식)
client.publish({ destination: '/app/subscribe/overseas', body: 'AAPL,NAS' });

// 데이터 수신 (3초마다 업데이트)
client.subscribe('/topic/overseas/AAPL', (message) => {
    const data = JSON.parse(message.body);
    // {symbol: "AAPL", price: "254.23", change: "1.41", changePercent: "0.56"}
});

// 구독 취소
client.publish({ destination: '/app/unsubscribe/overseas', body: 'AAPL,NAS' });
```

---

### 전체 연동 예시 (React)

```javascript
import { useEffect, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import axios from 'axios';

const NGROK_URL = 'https://xxxx.ngrok-free.dev'; // 백엔드 개발자에게 받은 주소로 교체

const api = axios.create({
    baseURL: NGROK_URL,
    headers: { 'ngrok-skip-browser-warning': 'true' }
});

api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

// 회원가입
const signUp = async (email, password, name) => {
    const res = await api.post('/api/users/signup', { email, password, name });
    return res.data;
};

// 로그인 → 토큰 저장
const login = async (email, password) => {
    const res = await api.post('/api/users/login', { email, password });
    localStorage.setItem('token', res.data.token);
    return res.data; // { token, name }
};

// 매수
const buy = async (symbol, name, market, quantity, price) => {
    const res = await api.post('/api/trade/buy', { symbol, name, market, type: 'BUY', quantity, price });
    return res.data;
};

// 매도
const sell = async (symbol, name, market, quantity, price) => {
    const res = await api.post('/api/trade/sell', { symbol, name, market, type: 'SELL', quantity, price });
    return res.data;
};

// 실시간 주가 컴포넌트
function StockPrice({ symbol, isOverseas = false, exchange = 'NAS' }) {
    const [price, setPrice] = useState(null);

    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(`${NGROK_URL}/ws`),
            onConnect: () => {
                if (isOverseas) {
                    client.publish({ destination: '/app/subscribe/overseas', body: `${symbol},${exchange}` });
                    client.subscribe(`/topic/overseas/${symbol}`, (message) => {
                        setPrice(JSON.parse(message.body));
                    });
                } else {
                    client.publish({ destination: '/app/subscribe/domestic', body: symbol });
                    client.subscribe(`/topic/domestic/${symbol}`, (message) => {
                        setPrice(JSON.parse(message.body));
                    });
                }
            }
        });
        client.activate();
        return () => {
            client.publish({
                destination: isOverseas ? '/app/unsubscribe/overseas' : '/app/unsubscribe/domestic',
                body: isOverseas ? `${symbol},${exchange}` : symbol
            });
            client.deactivate();
        };
    }, [symbol]);

    if (!price) return <div>로딩 중...</div>;
    return (
        <div>
            <h3>{price.symbol}</h3>
            <p>현재가: {isOverseas ? `$${price.price}` : `${Number(price.price).toLocaleString()}원`}</p>
            <p>전일대비: {price.change} ({price.changePercent}%)</p>
        </div>
    );
}
```

---

## TradingView Lightweight Charts 연동

```bash
npm install lightweight-charts
```

```javascript
import { createChart } from 'lightweight-charts';

const chart = createChart(document.getElementById('chart'), { width: 800, height: 400 });
const candleSeries = chart.addCandlestickSeries();

const chartData = await api.get('/api/stocks/chart/domestic/005930?period=D');
candleSeries.setData(chartData.data.map(item => ({
    time: `${item.date.substring(0,4)}-${item.date.substring(4,6)}-${item.date.substring(6,8)}`,
    open: parseFloat(item.open),
    high: parseFloat(item.high),
    low: parseFloat(item.low),
    close: parseFloat(item.close),
})));
```

---

## 주의사항

1. **ngrok 주소는 매번 바뀌어요.** 백엔드 개발자가 서버 재시작할 때마다 새 주소를 받아야 해요.
2. **ngrok-skip-browser-warning 헤더는 필수예요.**
3. **보호된 API 요청 시 `Authorization: Bearer {token}` 헤더 필수예요.** 토큰은 로그인 응답에서 받아 `localStorage`에 저장해두세요.
4. **로그인 후에만** 매매, 잔고, 보유종목, 주문내역, 관심종목 API를 사용할 수 있어요. 미로그인 시 401 응답이 와요.
5. **토큰 유효기간은 24시간**이에요. 만료 시 재로그인 필요해요.
5. **한국 주식 거래 시간**: 오전 9시 ~ 오후 3시 30분
6. **미국 주식 거래 시간**: 한국 시간 기준 밤 11시 30분 ~ 새벽 6시
7. **분봉 데이터는 장중에만 의미있는 데이터예요.**
8. **초기 가상 잔고는 1,000만원**이에요.

---

## 응답 데이터 필드 설명

### 현재가 데이터
| 필드 | 설명 | 예시 |
|------|------|------|
| symbol | 종목코드 | 005930, AAPL |
| price | 현재가 | 75000, 254.23 |
| change | 전일대비 변동가 | 1400, 1.41 |
| changePercent | 전일대비 등락률 | 1.90, 0.56 |

### 차트 데이터
| 필드 | 설명 |
|------|------|
| date | 날짜 (YYYYMMDD) 또는 연도 (YYYY) |
| open | 시가 |
| high | 고가 |
| low | 저가 |
| close | 종가 |
| volume | 거래량 |

### 검색 결과
| 필드 | 설명 | 예시 |
|------|------|------|
| symbol | 종목코드 | 005930, AAPL |
| name | 종목명 | 삼성전자, Apple Inc. |
| market | 시장구분 | KOSPI, NASDAQ, ETF 등 |

### 보유 종목
| 필드 | 설명 |
|------|------|
| symbol | 종목코드 |
| name | 종목명 |
| market | 시장 |
| quantity | 보유수량 |
| avgPrice | 평균매수가 |

### 주문 내역
| 필드 | 설명 |
|------|------|
| symbol | 종목코드 |
| name | 종목명 |
| type | 매수(BUY) / 매도(SELL) |
| quantity | 주문수량 |
| price | 체결가 |
| createdAt | 주문시간 |