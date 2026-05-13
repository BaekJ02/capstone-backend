package capstone.service;

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
                        .build());
            }
            if ("VOLUME".equals(type)) {
                result.sort((a, b) -> {
                    try {
                        return Long.compare(Long.parseLong(b.getVolume()), Long.parseLong(a.getVolume()));
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
        try {
            String endpoint;
            if ("FALL".equals(type)) {
                endpoint = "/biggest-losers";
            } else if ("VOLUME".equals(type)) {
                endpoint = "/most-actives";
            } else {
                endpoint = "/biggest-gainers";
            }

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

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : "";
    }
}
