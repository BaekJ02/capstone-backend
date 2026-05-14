# 캡스톤 기능 로드맵

---

## ✅ 현재 구현 완료

- KIS API 연동 (국내 + 미국 실시간 주가)
- 종목 검색 (국내 주식 / ETF / 미국 주식)
- WebSocket 실시간 주가 스트리밍
- REST API (현재가, 차트 데이터, 분봉, 연봉 등)
- 회원가입 / 로그인 / 로그아웃
- 모의 매수 / 매도
- 보유 종목 조회
- 잔고 조회
- 주문 내역 조회
- CORS 설정
- API 키 보안 처리
- H2 파일 기반 DB 연동
- 코스닥 종목 검색 버그 수정
- 관심종목 추가/삭제/조회
- 종목별 상세정보 조회 (국내주식: 시가총액, PER, EPS, PBR, BPS, 52주 최고/최저, 거래량, 거래대금 / 미국주식: 현재가, 전일대비, 등락률, 거래량)
- KIS 웹소켓 실시간 주가 연동 (국내주식 정규장)
- 시간대별 자동 전환 로직 (정규장/시간외 단일가/야간)
- MarketTimeService 구현
- KisWebSocketClient 구현
- 미국주식 KIS 웹소켓 실시간 연동 (HDFSCNT0, tr_key: D+거래소+심볼)
- 국내주식 호가창 REST API (FHKST01010200)
- 실시간 호가창 웹소켓 (H0STASP0)
- 실시간 체결 내역 웹소켓 (H0STCNT0 재활용)
- 시간외 단순화: 정규장 외 모든 시간 종가 고정 표시
- 매매 수량 음수/0 유효성 검사 (프론트+백엔드)
- N+1 쿼리 문제 해결
- 보유종목 실시간 DOM 업데이트 방식으로 변경
- Spring Security + JWT 인증 구현
- BCrypt 비밀번호 암호화
- JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig 구현
- 세션 방식 → JWT Stateless 방식으로 전환
- 서버 재시작해도 로그인 유지
- FMP API 연동 미국주식 재무데이터 (PER, PBR, 시가총액, 배당금, 52주 최고/최저)
- API URL: https://financialmodelingprep.com/stable/profile, /ratios-ttm
- 실현손익 조회 (DAY/WEEK/MONTH/YEAR/ALL 기간별)
- 환율/달러 잔고 기능 (한국수출입은행 API, 1시간 캐싱)
- 원화↔달러 환전 API
- 미국주식 달러 매매 (dollarBalance에서 차감/추가)
- 전체 계좌 현황 (원화+달러+보유주식 합산)
- 미국주식 금액 $ 표시
- 실시간 체결 매수/매도 구분 수정 (fields[offset+21], 1=매수 5=매도)
- 체결 데이터 다건 처리 (46개 필드 기준 offset 적용)
- 호가창 잔량 합계 수정
- 환율 API URL 변경 (oapi.koreaexim.go.kr)
- 미국주식 검색 개선 (대소문자 무관, 종목명 간소화 - Inc./Corp. 등 제거, "Apple (AAPL)" 형태)
- Hibernate 반복 로그 제거 (open-in-view=false, show-sql=false)
- 국내주식 호가창 REST API 10단계로 확장 (기존 5단계)
- 호가창 잔량 합계 표시 수정
- H2 → MySQL 교체 완료 (로컬 개발환경)
- Claude AI 챗봇 API 구현 (POST /api/ai/chat, 멀티턴 대화, claude-haiku-4-5-20251001 모델)
- AI 포트폴리오 분석 API 구현 (POST /api/ai/analyze/holdings, /portfolio, /recommend)
- AI 분석 시 실시간 현재가 기반 수익률 계산 (StockService 연동)
- 시장 순위 API 구현 (MarketRankingController, MarketRankingService, RankingItemDto)
  - 국내주식: KIS 등락률 순위(FHPST01700000, /ranking/fluctuation), 거래량 순위(FHPST01710000, /quotations/volume-rank)
  - 미국주식: FMP biggest-gainers / biggest-losers / most-actives
  - 엔드포인트: GET /api/market/domestic/ranking?type=, GET /api/market/overseas/ranking?type=
  - 인증 불필요 (SecurityConfig permitAll 처리)
- 국내주식 거래대금 순위 정확도 수정 (FID_BLNG_CLS_CODE 0→3, volume 필드 acml_vol→acml_tr_pbmn으로 변경, 실제 거래금액 기준 정렬)
- DB 스키마 정리 완료 (MySQL Workbench DDL 스크립트 추출 — users, holding, orders, watchlist 4개 테이블, 팀원 공유)

---

## ⚠️ 알려진 이슈 / 한계

- 국내주식 호가창 잔량이 실제 값의 약 1/2로 표시됨
  - REST API(FHKST01010200)와 웹소켓(H0STASP0) 모두 동일한 현상
  - KIS 오픈API 데이터 제공 방식의 한계로 추정 (토스증권 등 전문 금융기관과 다른 데이터)
  - 코드 자체는 정확하게 구현되어 있음
  - 향후 원인 파악 후 수정 예정 (KIS Developers Q&A 문의 고려)
- 정규장 마감 직전(15:27~15:30) KIS API 불안정 구간 존재
  - 해당 시간대에 웹소켓 데이터가 일시적으로 끊기거나 NaN 표시될 수 있음
  - 코드 버그가 아닌 KIS API 자체 특성 (동시호가 전환 구간)
- 미국주식 호가창 및 실시간 체결 데이터: KIS 오픈API 미제공으로 구현 불가
- 정규장 외 시간대 실시간 주가: KIS API 한계로 구현 불가 (3초 폴링 방식으로 대체 제공)

---

## 🔨 다음 할 것

1. 프론트엔드 연동 테스트 (다음주 예정)
2. AI 분석 출력 양식 고정 → 3D 큐빅 AI 모델 도입 후 진행 예정
3. 데일리 퀴즈 / 주식 시장 날씨 → UI 개편 후 고민
4. 호가창 잔량 1/2 문제 수정 (KIS API 한계로 해결 어려울 수 있음, 장중 테스트 필요)

---

## 🚀 앞으로 구현하면 좋을 것들

### 1. ~~시장 현황 기능~~ ✅ 완료

**TOP 거래대금 / 거래량 / 급상승 / 급하락**
- ~~실시간으로 거래량이 많은 종목, 급등/급락 종목 리스트 제공~~
- ~~KIS API에서 데이터 제공 가능~~
- 국내(KIS) + 미국(FMP) 모두 구현 완료, 로그인 불필요

---

### ~~2. 적립식 투자 시뮬레이터~~ ❌ 구현 안 하기로 결정

---

### 3. 주식 시장 날씨 🌤️

**시장 상황을 날씨로 직관적으로 표현**

| 날씨 | 의미 |
|------|------|
| ☀️ 맑음 | 활발하고 좋은 장 |
| ⛅ 구름 조금 | 평온한 횡보장 |
| 🌥️ 흐림 | 약보합, 관망 분위기 |
| 🌧️ 비 | 하락장, 투자 주의 |
| ⛈️ 폭풍 | 급락 + 변동성 심함 |

- 코스피 / 코스닥 / 나스닥 각각 날씨 표시 가능
- 나중에 AI 감성 분석과 연동하면 더 정교해짐

---

### ~~4. 투자 퀘스트 / 순위 시스템~~ ❌ 구현 안 하기로 결정

---

### ~~5. 플랫폼 내 경제 시스템~~ ❌ 구현 안 하기로 결정 (퀘스트/순위 시스템과 연동 구조였으므로 함께 제외)

---

### 6. 데일리 퀴즈 📚

**매일 주식 관련 퀴즈로 자연스럽게 지식 습득**

- AI가 매일 새로운 문제 자동 생성
- 맞추면 포인트 + 경험치 획득
- 예시: "PER이 낮을수록 저평가된 주식이다 O/X"
- 나중에 AI가 틀린 문제 기반으로 맞춤 학습 추천 가능

---

### ~~7. 소셜 트레이딩~~ ❌ 구현 안 하기로 결정

---

## 🤖 AI 기능 (일부 구현 중)

- ✅ AI 챗봇 (구현 완료 - Haiku 모델)
- ✅ AI 포트폴리오 분석 (종목 분석 / 포트폴리오 분석 / 섹터·종목 추천)
- ⏸️ AI 분석 출력 양식 고정 → 3D 큐빅 AI 모델 도입 후로 연기
- 종목 난이도 / 위험도 자동 분류
- 포트폴리오 다양성 점수 및 분석
- 투자 성향 분석 및 맞춤 종목 추천
- 시장 뉴스 감성 분석 → 주식 날씨 정교화
- 데일리 퀴즈 AI 자동 생성
- 매매 패턴 분석 및 피드백
- 중단기 주가 예측
- AI 종목 심층 분석 리포트 생성