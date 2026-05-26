package capstone.service;

import capstone.dto.IndexDto;
import capstone.dto.RankingItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketRankingService {

    private final RestTemplate restTemplate;
    private final KisAuthService kisAuthService;

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

    public List<RankingItemDto> getDomesticRanking(String type) {
        try {
            String baseUrl;
            String trId;
            String symbolField;
            Map<String, String> params = new LinkedHashMap<>();

            if ("VOLUME".equals(type)) {
                baseUrl = kisBaseUrl + "/uapi/domestic-stock/v1/quotations/volume-rank";
                trId = "FHPST01710000";
                symbolField = "mksc_shrn_iscd";
                params.put("FID_COND_MRKT_DIV_CODE", "J");
                params.put("FID_COND_SCR_DIV_CODE", "20171");
                params.put("FID_INPUT_ISCD", "0000");
                params.put("FID_DIV_CLS_CODE", "0");
                params.put("FID_BLNG_CLS_CODE", "3");
                params.put("FID_TRGT_CLS_CODE", "111111111");
                params.put("FID_TRGT_EXLS_CLS_CODE", "0000000000");
                params.put("FID_INPUT_PRICE_1", "");
                params.put("FID_INPUT_PRICE_2", "");
                params.put("FID_VOL_CNT", "");
            } else if ("MARKET_CAP".equals(type)) {
                baseUrl = kisBaseUrl + "/uapi/domestic-stock/v1/ranking/market-cap";
                trId = "FHPST01740000";
                symbolField = "mksc_shrn_iscd";
                params.put("fid_cond_mrkt_div_code", "J");
                params.put("fid_cond_scr_div_code", "20174");
                params.put("fid_div_cls_code", "0");
                params.put("fid_input_iscd", "0000");
                params.put("fid_trgt_cls_code", "0");
                params.put("fid_trgt_exls_cls_code", "0");
                params.put("fid_input_price_1", "");
                params.put("fid_input_price_2", "");
                params.put("fid_vol_cnt", "");
            } else {
                baseUrl = kisBaseUrl + "/uapi/domestic-stock/v1/ranking/fluctuation";
                trId = "FHPST01700000";
                symbolField = "stck_shrn_iscd";
                params.put("fid_cond_mrkt_div_code", "J");
                params.put("fid_cond_scr_div_code", "20170");
                params.put("fid_input_iscd", "0000");
                params.put("fid_rank_sort_cls_code", "FALL".equals(type) ? "1" : "0");
                params.put("fid_input_cnt_1", "0");
                params.put("fid_prc_cls_code", "0");
                params.put("fid_input_price_1", "");
                params.put("fid_input_price_2", "");
                params.put("fid_vol_cnt", "");
                params.put("fid_trgt_cls_code", "0");
                params.put("fid_trgt_exls_cls_code", "0");
                params.put("fid_div_cls_code", "0");
                params.put("fid_rsfl_rate1", "");
                params.put("fid_rsfl_rate2", "");
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
            params.forEach(builder::queryParam);
            String finalUrl = builder.toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", trId);
            headers.set("custtype", "P");

            log.info("KIS 국내 순위 호출 [type={}] URL: {}", type, finalUrl);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(finalUrl, HttpMethod.GET, request, Map.class).getBody();

            List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
            if (output == null) return new ArrayList<>();

            List<RankingItemDto> result = new ArrayList<>();
            for (Map<String, Object> item : output) {
                result.add(RankingItemDto.builder()
                        .symbol(str(item, symbolField))
                        .name(str(item, "hts_kor_isnm"))
                        .price(str(item, "stck_prpr"))
                        .change(str(item, "prdy_vrss"))
                        .changePercent(str(item, "prdy_ctrt"))
                        .volume(str(item, "VOLUME".equals(type) ? "acml_tr_pbmn" : "acml_vol"))
                        .marketCap("MARKET_CAP".equals(type) ? str(item, "stck_avls") : "")
                        .build());
            }
            if ("VOLUME".equals(type)) {
                result.sort((a, b) -> {
                    try {
                        return Long.compare(Long.parseLong(b.getVolume()), Long.parseLong(a.getVolume()));
                    } catch (Exception e) { return 0; }
                });
            } else if ("MARKET_CAP".equals(type)) {
                result.sort((a, b) -> {
                    try {
                        return Long.compare(Long.parseLong(b.getMarketCap()), Long.parseLong(a.getMarketCap()));
                    } catch (Exception e) { return 0; }
                });
            } else {
                result.sort((a, b) -> {
                    try {
                        double va = Double.parseDouble(a.getChangePercent());
                        double vb = Double.parseDouble(b.getChangePercent());
                        return "FALL".equals(type) ? Double.compare(va, vb) : Double.compare(vb, va);
                    } catch (Exception e) { return 0; }
                });
            }
            return result;
        } catch (Exception e) {
            log.error("국내 순위 조회 실패 [type={}]: {}", type, e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<RankingItemDto> getOverseasRanking(String type) {
        if ("VOLUME".equals(type)) {
            return getOverseasVolumeRankingFromKis();
        }
        if ("MARKET_CAP".equals(type)) {
            return getOverseasMarketCapRankingFromKis();
        }

        try {
            String endpoint = "FALL".equals(type) ? "/biggest-losers" : "/biggest-gainers";
            String url = fmpBaseUrl + endpoint + "?apikey=" + fmpApiKey;
            List<Map<String, Object>> list = restTemplate.getForObject(url, List.class);
            if (list == null) return new ArrayList<>();

            List<RankingItemDto> result = new ArrayList<>();
            List<Map<String, Object>> top20 = list.subList(0, Math.min(20, list.size()));
            for (Map<String, Object> item : top20) {
                Object vol = item.get("volume");
                result.add(RankingItemDto.builder()
                        .symbol(str(item, "symbol"))
                        .name(str(item, "name"))
                        .price(String.valueOf(item.get("price")))
                        .change(String.valueOf(item.get("change")))
                        .changePercent(String.valueOf(item.get("changesPercentage")))
                        .volume(vol != null ? String.valueOf(vol) : "")
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.error("미국 순위 조회 실패 [type={}]: {}", type, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<RankingItemDto> getOverseasVolumeRankingFromKis() {
        String baseUrl = kisBaseUrl + "/uapi/overseas-stock/v1/ranking/trade-pbmn";
        String[] exchanges = {"NAS", "NYS", "AMS"};

        List<RankingItemDto> combined = new ArrayList<>();
        for (String excd : exchanges) {
            try {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                        .queryParam("KEYB", "")
                        .queryParam("AUTH", "")
                        .queryParam("EXCD", excd)
                        .queryParam("NDAY", "0")
                        .queryParam("VOL_RANG", "0")
                        .queryParam("PRC1", "")
                        .queryParam("PRC2", "");
                String finalUrl = builder.toUriString();

                HttpHeaders headers = new HttpHeaders();
                headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
                headers.set("appkey", appKey);
                headers.set("appsecret", appSecret);
                headers.set("tr_id", "HHDFS76320010");
                headers.set("custtype", "P");

                HttpEntity<Void> request = new HttpEntity<>(headers);
                Map<String, Object> response = restTemplate.exchange(finalUrl, HttpMethod.GET, request, Map.class).getBody();
                if (response == null) continue;

                List<Map<String, Object>> output2 = (List<Map<String, Object>>) response.get("output2");
                if (output2 == null) continue;

                for (Map<String, Object> item : output2) {
                    combined.add(RankingItemDto.builder()
                            .symbol(str(item, "symb"))
                            .name(str(item, "name"))
                            .price(str(item, "last"))
                            .change(str(item, "diff"))
                            .changePercent(str(item, "rate"))
                            .volume(str(item, "tamt"))
                            .build());
                }
            } catch (Exception e) {
                log.error("미국 거래대금 순위 조회 실패 [excd={}]: {}", excd, e.getMessage());
            }
        }

        if (combined.isEmpty()) return combined;

        combined.sort((a, b) -> {
            try {
                return Double.compare(Double.parseDouble(b.getVolume()), Double.parseDouble(a.getVolume()));
            } catch (Exception e) { return 0; }
        });
        return combined.subList(0, Math.min(20, combined.size()));
    }

    private List<RankingItemDto> getOverseasMarketCapRankingFromKis() {
        String baseUrl = kisBaseUrl + "/uapi/overseas-stock/v1/ranking/market-cap";
        String[] exchanges = {"NAS", "NYS", "AMS"};

        List<RankingItemDto> combined = new ArrayList<>();
        for (String excd : exchanges) {
            try {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                        .queryParam("KEYB", "")
                        .queryParam("AUTH", "")
                        .queryParam("EXCD", excd)
                        .queryParam("VOL_RANG", "0");
                String finalUrl = builder.toUriString();

                HttpHeaders headers = new HttpHeaders();
                headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
                headers.set("appkey", appKey);
                headers.set("appsecret", appSecret);
                headers.set("tr_id", "HHDFS76350100");
                headers.set("custtype", "P");

                HttpEntity<Void> request = new HttpEntity<>(headers);
                Map<String, Object> response = restTemplate.exchange(finalUrl, HttpMethod.GET, request, Map.class).getBody();
                if (response == null) continue;

                List<Map<String, Object>> output2 = (List<Map<String, Object>>) response.get("output2");
                if (output2 == null) continue;

                for (Map<String, Object> item : output2) {
                    combined.add(RankingItemDto.builder()
                            .symbol(str(item, "symb"))
                            .name(str(item, "name"))
                            .price(str(item, "last"))
                            .change(str(item, "diff"))
                            .changePercent(str(item, "rate"))
                            .volume(str(item, "tvol"))
                            .marketCap(str(item, "tomv"))
                            .build());
                }
            } catch (Exception e) {
                log.error("미국 시가총액 순위 조회 실패 [excd={}]: {}", excd, e.getMessage());
            }
        }

        if (combined.isEmpty()) return combined;

        combined.sort((a, b) -> {
            try {
                return Double.compare(Double.parseDouble(b.getMarketCap()), Double.parseDouble(a.getMarketCap()));
            } catch (Exception e) { return 0; }
        });
        return combined.subList(0, Math.min(20, combined.size()));
    }

    public List<IndexDto> getMarketIndices() {
        List<IndexDto> result = new ArrayList<>();
        result.add(getDomesticIndex("0001", "KOSPI", "코스피"));
        result.add(getDomesticIndex("1001", "KOSDAQ", "코스닥"));
        result.add(getOverseasIndex("SPX", "SP500", "S&P 500"));
        result.add(getOverseasIndex("COMP", "NASDAQ", "나스닥"));
        return result;
    }

    private IndexDto getDomesticIndex(String iscd, String code, String name) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(kisBaseUrl + "/uapi/domestic-stock/v1/quotations/inquire-index-price")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                    .queryParam("FID_INPUT_ISCD", iscd)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "FHPUP02100000");
            headers.set("custtype", "P");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();
            Map<String, Object> output = (Map<String, Object>) response.get("output");

            return IndexDto.builder()
                    .code(code)
                    .name(name)
                    .price(str(output, "bstp_nmix_prpr"))
                    .change(str(output, "bstp_nmix_prdy_vrss"))
                    .changePercent(str(output, "bstp_nmix_prdy_ctrt"))
                    .build();
        } catch (Exception e) {
            log.error("국내 지수 조회 실패 [{}]: {}", code, e.getMessage());
            return IndexDto.builder().code(code).name(name).price("").change("").changePercent("").build();
        }
    }

    private IndexDto getOverseasIndex(String iscd, String code, String name) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(kisBaseUrl + "/uapi/overseas-price/v1/quotations/inquire-time-indexchartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "N")
                    .queryParam("FID_INPUT_ISCD", iscd)
                    .queryParam("FID_HOUR_CLS_CODE", "0")
                    .queryParam("FID_PW_DATA_INCU_YN", "N")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", "FHKST03030200");
            headers.set("custtype", "P");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();
            Map<String, Object> output1 = (Map<String, Object>) response.get("output1");

            return IndexDto.builder()
                    .code(code)
                    .name(name)
                    .price(str(output1, "ovrs_nmix_prpr"))
                    .change(str(output1, "ovrs_nmix_prdy_vrss"))
                    .changePercent(str(output1, "prdy_ctrt"))
                    .build();
        } catch (Exception e) {
            log.error("해외 지수 조회 실패 [{}]: {}", code, e.getMessage());
            return IndexDto.builder().code(code).name(name).price("").change("").changePercent("").build();
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : "";
    }
}
