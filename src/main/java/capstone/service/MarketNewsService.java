package capstone.service;

import capstone.dto.MarketNewsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketNewsService {

    private final RestTemplate restTemplate;
    private final KisAuthService kisAuthService;
    private final ObjectMapper objectMapper;

    @Value("${kis.api.url}")
    private String kisBaseUrl;

    @Value("${kis.api.key}")
    private String appKey;

    @Value("${kis.api.secret}")
    private String appSecret;

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @Value("${naver.api.url}")
    private String naverApiUrl;

    @Value("${naver.client.id}")
    private String naverClientId;

    @Value("${naver.client.secret}")
    private String naverClientSecret;

    private MarketNewsDto cachedOverseasNews = null;
    private MarketNewsDto cachedDomesticNews = null;

    @PostConstruct
    public void init() {
        refreshOverseasNews();
        refreshDomesticNews();
    }

    // ── 해외 뉴스 스케줄 ──
    @Scheduled(cron = "0 0 8,14,16,20 * * *")
    public void refreshOverseasNews() {
        try {
            log.info("해외 시장 뉴스 갱신 시작");
            List<String> newsTitles = fetchOverseasNewsTitles();
            List<Map<String, String>> topStocks = fetchTopOverseasStocks();
            cachedOverseasNews = analyzeWithClaude(newsTitles, topStocks, "overseas");
            log.info("해외 시장 뉴스 갱신 완료");
        } catch (Exception e) {
            log.error("해외 시장 뉴스 갱신 실패: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 22 * * *")
    public void refreshOverseasNewsNightOpen() { refreshOverseasNews(); }

    @Scheduled(cron = "0 0 5 * * *")
    public void refreshOverseasNewsNightClose() { refreshOverseasNews(); }

    // ── 국내 뉴스 스케줄 ──
    @Scheduled(cron = "0 0 8,14,16,20 * * *")
    public void refreshDomesticNews() {
        try {
            log.info("국내 시장 뉴스 갱신 시작");
            List<String> newsTitles = fetchDomesticNewsTitles();
            List<Map<String, String>> topStocks = fetchTopDomesticStocks();
            cachedDomesticNews = analyzeWithClaude(newsTitles, topStocks, "domestic");
            log.info("국내 시장 뉴스 갱신 완료");
        } catch (Exception e) {
            log.error("국내 시장 뉴스 갱신 실패: {}", e.getMessage());
        }
    }

    public MarketNewsDto getOverseasNews() { return cachedOverseasNews; }
    public MarketNewsDto getDomesticNews() { return cachedDomesticNews; }

    // ── 해외 뉴스 제목 수집 (KIS) ──
    private List<String> fetchOverseasNewsTitles() {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(kisBaseUrl + "/uapi/overseas-price/v1/quotations/news-title")
                    .queryParam("INFO_GB", "")
                    .queryParam("CLASS_CD", "")
                    .queryParam("NATION_CD", "US")
                    .queryParam("EXCHANGE_CD", "")
                    .queryParam("SYMB", "")
                    .queryParam("DATA_DT", "")
                    .queryParam("DATA_TM", "")
                    .queryParam("CTS", "")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "HHPSTH60100C1");
            headers.set("custtype", "P");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

            List<Map<String, Object>> outblock1 = (List<Map<String, Object>>) response.get("outblock1");
            if (outblock1 == null) return new ArrayList<>();

            List<String> titles = new ArrayList<>();
            for (int i = 0; i < Math.min(10, outblock1.size()); i++) {
                String title = (String) outblock1.get(i).get("title");
                if (title != null && !title.isBlank()) titles.add(title);
            }
            log.info("해외 뉴스 제목 {}건 수집", titles.size());
            return titles;
        } catch (Exception e) {
            log.error("해외 뉴스 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 국내 뉴스 제목 수집 (네이버) ──
    private List<String> fetchDomesticNewsTitles() {
        try {
            List<String> titles = new ArrayList<>();
            String[] keywords = {"코스피 증시", "국내 주식 시황"};

            for (String keyword : keywords) {
                String url = UriComponentsBuilder
                        .fromUriString(naverApiUrl)
                        .queryParam("query", keyword)
                        .queryParam("display", 5)
                        .queryParam("sort", "date")
                        .toUriString();

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Naver-Client-Id", naverClientId);
                headers.set("X-Naver-Client-Secret", naverClientSecret);

                HttpEntity<Void> request = new HttpEntity<>(headers);
                Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items != null) {
                    for (Map<String, Object> item : items) {
                        String title = (String) item.get("title");
                        if (title != null) {
                            title = title.replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
                            titles.add(title);
                        }
                    }
                }
            }
            log.info("국내 뉴스 제목 {}건 수집", titles.size());
            return titles;
        } catch (Exception e) {
            log.error("국내 뉴스 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 해외 거래대금 상위 종목 ──
    private List<Map<String, String>> fetchTopOverseasStocks() {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(kisBaseUrl + "/uapi/overseas-stock/v1/ranking/trade-pbmn")
                    .queryParam("KEYB", "")
                    .queryParam("AUTH", "")
                    .queryParam("EXCD", "NAS")
                    .queryParam("NDAY", "0")
                    .queryParam("VOL_RANG", "0")
                    .queryParam("PRC1", "")
                    .queryParam("PRC2", "")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "HHDFS76320010");
            headers.set("custtype", "P");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

            List<Map<String, Object>> output2 = (List<Map<String, Object>>) response.get("output2");
            if (output2 == null) return new ArrayList<>();

            List<Map<String, String>> stocks = new ArrayList<>();
            for (int i = 0; i < Math.min(20, output2.size()); i++) {
                Map<String, Object> item = output2.get(i);
                Map<String, String> stock = new HashMap<>();
                stock.put("symbol", String.valueOf(item.get("symb")));
                stock.put("name", String.valueOf(item.get("name")));
                stock.put("changePercent", String.valueOf(item.get("rate")));
                stocks.add(stock);
            }
            return stocks;
        } catch (Exception e) {
            log.error("해외 거래대금 상위 종목 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 국내 거래대금 상위 종목 (KIS) ──
    private List<Map<String, String>> fetchTopDomesticStocks() {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(kisBaseUrl + "/uapi/domestic-stock/v1/quotations/volume-rank")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                    .queryParam("FID_INPUT_ISCD", "0000")
                    .queryParam("FID_DIV_CLS_CODE", "0")
                    .queryParam("FID_BLNG_CLS_CODE", "3")
                    .queryParam("FID_TRGT_CLS_CODE", "111111111")
                    .queryParam("FID_TRGT_EXLS_CLS_CODE", "0000000000")
                    .queryParam("FID_INPUT_PRICE_1", "")
                    .queryParam("FID_INPUT_PRICE_2", "")
                    .queryParam("FID_VOL_CNT", "")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "FHPST01710000");
            headers.set("custtype", "P");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

            List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
            if (output == null) return new ArrayList<>();

            List<Map<String, String>> stocks = new ArrayList<>();
            for (int i = 0; i < Math.min(20, output.size()); i++) {
                Map<String, Object> item = output.get(i);
                Map<String, String> stock = new HashMap<>();
                stock.put("symbol", String.valueOf(item.get("mksc_shrn_iscd")));
                stock.put("name", String.valueOf(item.get("hts_kor_isnm")));
                stock.put("changePercent", String.valueOf(item.get("prdy_ctrt")));
                stocks.add(stock);
            }
            return stocks;
        } catch (Exception e) {
            log.error("국내 거래대금 상위 종목 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Claude 분석 ──
    private MarketNewsDto analyzeWithClaude(List<String> newsTitles, List<Map<String, String>> topStocks, String type) {
        try {
            String marketName = "domestic".equals(type) ? "국내" : "미국";
            StringBuilder prompt = new StringBuilder();
            prompt.append("다음은 오늘의 ").append(marketName).append(" 주식 시장 뉴스 제목과 거래대금 상위 종목이야.\n\n");
            prompt.append("【뉴스 제목】\n");
            for (String title : newsTitles) {
                prompt.append("- ").append(title).append("\n");
            }
            prompt.append("\n【거래대금 상위 종목】\n");
            for (Map<String, String> stock : topStocks) {
                prompt.append("- ").append(stock.get("symbol"))
                        .append(" (").append(stock.get("name")).append(")")
                        .append(" ").append(stock.get("changePercent")).append("%\n");
            }
            prompt.append("\n위 정보를 바탕으로 반드시 아래 JSON 형식으로만 응답해. 다른 텍스트는 절대 포함하지 마.\n");
            prompt.append("""
                {
                  "weather": "SUNNY 또는 CLOUDY 또는 RAINY 또는 STORM",
                  "headlines": ["뉴스 핵심 요약 1", "뉴스 핵심 요약 2", "뉴스 핵심 요약 3"],
                  "positive": {
                    "sector": "호재 섹터명",
                    "reason": "상승 이유 한두 문장",
                    "stocks": [
                      {"symbol": "종목코드", "name": "종목명", "changePercent": "+X.X%"},
                      {"symbol": "종목코드", "name": "종목명", "changePercent": "+X.X%"},
                      {"symbol": "종목코드", "name": "종목명", "changePercent": "+X.X%"}
                    ]
                  },
                  "negative": {
                    "sector": "악재 섹터명",
                    "reason": "하락 이유 한두 문장",
                    "stocks": [
                      {"symbol": "종목코드", "name": "종목명", "changePercent": "-X.X%"},
                      {"symbol": "종목코드", "name": "종목명", "changePercent": "-X.X%"},
                      {"symbol": "종목코드", "name": "종목명", "changePercent": "-X.X%"}
                    ]
                  },
                  "summary": "시장 전반적인 분위기 한 문장 요약"
                }
                """);
            prompt.append("weather 기준: SUNNY(상승장, 긍정적), CLOUDY(혼조세), RAINY(하락장), STORM(급락/패닉).");
            prompt.append("호재/악재가 명확하지 않으면 그나마 가장 관련있는 섹터로 판단해줘. positive와 negative 모두 반드시 포함해야 해.");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "claude-haiku-4-5-20251001");
            requestBody.put("max_tokens", 1000);
            requestBody.put("system", "당신은 주식 시장 분석 전문가입니다. 요청된 JSON 형식으로만 응답하세요. 마크다운 코드블록이나 추가 텍스트 없이 순수 JSON만 반환하세요.");
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt.toString())));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", claudeApiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.exchange(
                    "https://api.anthropic.com/v1/messages",
                    HttpMethod.POST, request, Map.class).getBody();

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            String jsonText = (String) content.get(0).get("text");
            jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();

            Map<String, Object> parsed = objectMapper.readValue(jsonText, Map.class);

            String weather = (String) parsed.get("weather");
            List<String> headlines = (List<String>) parsed.get("headlines");
            Map<String, Object> positiveMap = (Map<String, Object>) parsed.get("positive");
            Map<String, Object> negativeMap = (Map<String, Object>) parsed.get("negative");
            String summary = (String) parsed.get("summary");

            return MarketNewsDto.builder()
                    .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .weather(weather)
                    .headlines(headlines)
                    .positive(parseSector(positiveMap))
                    .negative(parseSector(negativeMap))
                    .summary(summary)
                    .build();

        } catch (Exception e) {
            log.error("Claude 분석 실패: {}", e.getMessage());
            return null;
        }
    }

    private MarketNewsDto.SectorDto parseSector(Map<String, Object> map) {
        if (map == null) return null;
        String sector = (String) map.get("sector");
        String reason = (String) map.get("reason");
        List<Map<String, Object>> stockList = (List<Map<String, Object>>) map.get("stocks");
        List<MarketNewsDto.StockDto> stocks = new ArrayList<>();
        if (stockList != null) {
            for (Map<String, Object> s : stockList) {
                stocks.add(MarketNewsDto.StockDto.builder()
                        .symbol((String) s.get("symbol"))
                        .name((String) s.get("name"))
                        .changePercent((String) s.get("changePercent"))
                        .build());
            }
        }
        return MarketNewsDto.SectorDto.builder().sector(sector).reason(reason).stocks(stocks).build();
    }
}
