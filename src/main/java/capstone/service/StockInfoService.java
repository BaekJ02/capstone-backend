package capstone.service;

import capstone.dto.StockInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInfoService {

    private final RestTemplate restTemplate;
    private final KisAuthService kisAuthService;
    private final WebClient webClient;

    @Value("${kis.api.url}")
    private String kisBaseUrl;

    @Value("${kis.api.key}")
    private String appKey;

    @Value("${kis.api.secret}")
    private String appSecret;

    @Value("${fmp.api.url}")
    private String fmpBaseUrl;

    @Value("${fmp.api.key}")
    private String fmpApiKey;

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @Value("${claude.api.url}")
    private String claudeApiUrl;

    private static final Set<String> DOMESTIC_MARKETS = Set.of("KOSPI", "KOSDAQ", "ETF");

    public StockInfoDto getStockInfo(String symbol, String market) {
        if (isDomestic(market)) {
            return getDomesticStockInfo(symbol, market);
        } else {
            return getOverseasStockInfo(symbol, market);
        }
    }

    private boolean isDomestic(String market) {
        return market != null && DOMESTIC_MARKETS.contains(market.toUpperCase());
    }

    private StockInfoDto getDomesticStockInfo(String symbol, String market) {
        String per = "";
        String pbr = "";
        String marketCap = "";
        String marketCapRank = "";
        String name = symbol;

        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String url = UriComponentsBuilder
                    .fromUriString(kisBaseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", symbol)
                    .queryParam("FID_INPUT_DATE_1", today)
                    .queryParam("FID_INPUT_DATE_2", today)
                    .queryParam("FID_PERIOD_DIV_CODE", "D")
                    .queryParam("FID_ORG_ADJ_PRC", "0")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "FHKST03010100");
            headers.set("custtype", "P");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

            if (response != null) {
                Map<String, Object> output1 = (Map<String, Object>) response.get("output1");
                if (output1 != null) {
                    per = str(output1, "per");
                    pbr = str(output1, "pbr");
                    String htsAvls = str(output1, "hts_avls");
                    marketCap = formatDomesticMarketCap(htsAvls);
                    name = str(output1, "hts_kor_isnm");
                    if (name.isEmpty()) name = symbol;
                }
            }
        } catch (Exception e) {
            log.error("국내 차트 API 조회 실패 [{}]: {}", symbol, e.getMessage());
        }

        try {
            String rankUrl = UriComponentsBuilder
                    .fromUriString(kisBaseUrl + "/uapi/domestic-stock/v1/ranking/market-cap")
                    .queryParam("fid_cond_mrkt_div_code", "J")
                    .queryParam("fid_cond_scr_div_code", "20174")
                    .queryParam("fid_div_cls_code", "0")
                    .queryParam("fid_input_iscd", "0000")
                    .queryParam("fid_trgt_cls_code", "0")
                    .queryParam("fid_trgt_exls_cls_code", "0")
                    .queryParam("fid_input_price_1", "")
                    .queryParam("fid_input_price_2", "")
                    .queryParam("fid_vol_cnt", "")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "FHPST01740000");
            headers.set("custtype", "P");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(rankUrl, HttpMethod.GET, request, Map.class).getBody();

            if (response != null) {
                List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
                if (output != null) {
                    for (int i = 0; i < output.size(); i++) {
                        Map<String, Object> item = output.get(i);
                        if (symbol.equals(str(item, "mksc_shrn_iscd"))) {
                            marketCapRank = market + " " + (i + 1) + "위";
                            if (name.equals(symbol)) {
                                String n = str(item, "hts_kor_isnm");
                                if (!n.isEmpty()) name = n;
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("국내 시총 순위 조회 실패 [{}]: {}", symbol, e.getMessage());
        }

        String aiAnalysis = callClaudeForAnalysis(name, market, marketCap, marketCapRank, per, pbr);

        return StockInfoDto.builder()
                .symbol(symbol)
                .name(name)
                .market(market)
                .marketCap(marketCap)
                .marketCapRank(marketCapRank)
                .per(per)
                .pbr(pbr)
                .aiAnalysis(aiAnalysis)
                .build();
    }

    private StockInfoDto getOverseasStockInfo(String symbol, String market) {
        String per = "";
        String pbr = "";
        String marketCap = "";
        String marketCapRank = "";
        String name = symbol;

        try {
            String ratiosUrl = fmpBaseUrl + "/ratios-ttm?symbol=" + symbol + "&apikey=" + fmpApiKey;
            List<Map<String, Object>> ratiosList = restTemplate.getForObject(ratiosUrl, List.class);
            if (ratiosList != null && !ratiosList.isEmpty()) {
                Map<String, Object> ratios = ratiosList.get(0);
                per = formatRatio(ratios.get("peRatioTTM"));
                pbr = formatRatio(ratios.get("priceToBookRatioTTM"));
            }
        } catch (Exception e) {
            log.error("FMP ratios-ttm 조회 실패 [{}]: {}", symbol, e.getMessage());
        }

        try {
            String profileUrl = fmpBaseUrl + "/profile?symbol=" + symbol + "&apikey=" + fmpApiKey;
            List<Map<String, Object>> profileList = restTemplate.getForObject(profileUrl, List.class);
            if (profileList != null && !profileList.isEmpty()) {
                Map<String, Object> profile = profileList.get(0);
                Object cap = profile.get("marketCap");
                marketCap = formatOverseasMarketCap(cap);
                String n = str(profile, "companyName");
                if (!n.isEmpty()) name = n;
            }
        } catch (Exception e) {
            log.error("FMP profile 조회 실패 [{}]: {}", symbol, e.getMessage());
        }

        try {
            String[] exchanges = {"NAS", "NYS", "AMS"};
            List<Map<String, Object>> combined = new java.util.ArrayList<>();
            for (String excd : exchanges) {
                try {
                    String rankUrl = UriComponentsBuilder
                            .fromUriString(kisBaseUrl + "/uapi/overseas-stock/v1/ranking/market-cap")
                            .queryParam("KEYB", "")
                            .queryParam("AUTH", "")
                            .queryParam("EXCD", excd)
                            .queryParam("VOL_RANG", "0")
                            .queryParam("CURR_GB", "0")
                            .toUriString();

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
                    headers.set("appkey", appKey);
                    headers.set("appsecret", appSecret);
                    headers.set("tr_id", "HHDFS76350100");
                    headers.set("custtype", "P");

                    HttpEntity<Void> request = new HttpEntity<>(headers);
                    Map<String, Object> response = restTemplate.exchange(rankUrl, HttpMethod.GET, request, Map.class).getBody();
                    if (response == null) continue;

                    List<Map<String, Object>> output2 = (List<Map<String, Object>>) response.get("output2");
                    if (output2 != null) combined.addAll(output2);
                } catch (Exception e) {
                    log.warn("미국 시총 순위 조회 실패 [excd={}]: {}", excd, e.getMessage());
                }
            }

            combined.sort((a, b) -> {
                try {
                    return Double.compare(
                            Double.parseDouble(str(b, "tomv")),
                            Double.parseDouble(str(a, "tomv")));
                } catch (Exception e) { return 0; }
            });

            for (int i = 0; i < combined.size(); i++) {
                if (symbol.equalsIgnoreCase(str(combined.get(i), "symb"))) {
                    marketCapRank = "미국주식 " + (i + 1) + "위";
                    break;
                }
            }
        } catch (Exception e) {
            log.error("미국 시총 순위 조회 실패 [{}]: {}", symbol, e.getMessage());
        }

        String aiAnalysis = callClaudeForAnalysis(name, market, marketCap, marketCapRank, per, pbr);

        return StockInfoDto.builder()
                .symbol(symbol)
                .name(name)
                .market(market)
                .marketCap(marketCap)
                .marketCapRank(marketCapRank)
                .per(per)
                .pbr(pbr)
                .aiAnalysis(aiAnalysis)
                .build();
    }

    private String callClaudeForAnalysis(String name, String market, String marketCap, String marketCapRank, String per, String pbr) {
        boolean domestic = isDomestic(market);
        String currencyNote = domestic ? "" : " (달러 기준으로 작성)";
        String prompt = String.format(
                "당신은 주식 분석 전문가입니다. 아래 데이터를 바탕으로 해당 종목에 대해 간결하게 3~4문장으로 설명해주세요.%n" +
                "반드시 아래 형식을 따르세요:%n" +
                "[종목명]은(는) [주요 사업 한 문장 설명]하는 기업입니다.%n" +
                "시가총액은 [시가총액]으로 [시총순위]입니다.%n" +
                "PER은 [PER]배, PBR은 [PBR]배로, [고평가/적정/저평가] 수준입니다.%n" +
                "[최근 주가 흐름이나 업종 특성에 대한 한 문장 코멘트]%n%n" +
                "주의사항:%n" +
                "- 숫자는 제공된 데이터만 사용할 것 (임의로 수치 생성 금지)%n" +
                "- 평가 기준: PER 10 미만=저평가, 10~25=적정, 25 초과=고평가 (업종 특성 고려)%n" +
                "- 반드시 한국어로 작성%n" +
                "- 4문장을 초과하지 말 것%s%n%n" +
                "종목명: %s%n" +
                "시가총액: %s%n" +
                "시총순위: %s%n" +
                "PER: %s%n" +
                "PBR: %s%n" +
                "시장: %s",
                currencyNote, name,
                marketCap.isEmpty() ? "정보 없음" : marketCap,
                marketCapRank.isEmpty() ? "정보 없음" : marketCapRank,
                per.isEmpty() ? "정보 없음" : per,
                pbr.isEmpty() ? "정보 없음" : pbr,
                market
        );

        Map<String, Object> requestBody = Map.of(
                "model", "claude-haiku-4-5-20251001",
                "max_tokens", 500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            Map<?, ?> response = webClient.post()
                    .uri(claudeApiUrl + "/v1/messages")
                    .header("x-api-key", claudeApiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            return (String) content.get(0).get("text");
        } catch (Exception e) {
            log.error("Claude AI 분석 실패 [{}]: {}", name, e.getMessage());
            return "AI 분석 중 오류가 발생했습니다.";
        }
    }

    private String formatDomesticMarketCap(String htsAvls) {
        if (htsAvls == null || htsAvls.isEmpty()) return "";
        try {
            long eok = Long.parseLong(htsAvls.trim());
            long jo = eok / 10000;
            long rem = eok % 10000;
            if (jo > 0 && rem > 0) {
                return String.format("%,d조 %,d억원", jo, rem);
            } else if (jo > 0) {
                return String.format("%,d조원", jo);
            } else {
                return String.format("%,d억원", rem);
            }
        } catch (Exception e) {
            return htsAvls;
        }
    }

    private String formatOverseasMarketCap(Object capObj) {
        if (capObj == null) return "";
        try {
            double cap = Double.parseDouble(String.valueOf(capObj));
            double trillion = cap / 1_000_000_000_000.0;
            double billion = cap / 1_000_000_000.0;
            if (trillion >= 1.0) {
                return String.format("$%.1f조", trillion);
            } else if (billion >= 1.0) {
                return String.format("$%.1f억", billion);
            } else {
                return String.format("$%.0f", cap);
            }
        } catch (Exception e) {
            return String.valueOf(capObj);
        }
    }

    private String formatRatio(Object val) {
        if (val == null) return "";
        try {
            double d = Double.parseDouble(String.valueOf(val));
            if (Double.isNaN(d) || Double.isInfinite(d)) return "";
            return String.format("%.2f", d);
        } catch (Exception e) {
            return "";
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : "";
    }
}
