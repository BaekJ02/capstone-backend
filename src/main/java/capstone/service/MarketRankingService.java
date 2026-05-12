package capstone.service;

import capstone.dto.RankingItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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
            String trId;
            String scrDivCode;
            String sortClsCode;

            if ("FALL".equals(type)) {
                trId = "FHPST01700000";
                scrDivCode = "20170";
                sortClsCode = "1";
            } else if ("VOLUME".equals(type)) {
                trId = "FHPST01710000";
                scrDivCode = "20171";
                sortClsCode = "0";
            } else {
                trId = "FHPST01700000";
                scrDivCode = "20170";
                sortClsCode = "0";
            }

            String endpoint = "VOLUME".equals(type)
                    ? "/uapi/domestic-stock/v1/quotations/volume-rank"
                    : "/uapi/domestic-stock/v1/quotations/inquire-price-rank";

            String url = kisBaseUrl + endpoint
                    + "?fid_cond_mrkt_div_code=J"
                    + "&fid_cond_scr_div_code=" + scrDivCode
                    + "&fid_input_iscd=0000"
                    + "&fid_rank_sort_cls_code=" + sortClsCode
                    + "&fid_input_cnt_1=0"
                    + "&fid_prc_cls_code=0"
                    + "&fid_input_price_1="
                    + "&fid_input_price_2="
                    + "&fid_vol_cnt="
                    + "&fid_trgt_cls_code=0"
                    + "&fid_trgt_exls_cls_code=0"
                    + "&fid_div_cls_code=0"
                    + "&fid_rsfl_rate1="
                    + "&fid_rsfl_rate2=";

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", trId);
            headers.set("custtype", "P");

            log.info("KIS 국내 순위 호출 [type={}] URL: {}", type, url);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

            List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
            if (output == null) return new ArrayList<>();

            List<RankingItemDto> result = new ArrayList<>();
            for (Map<String, Object> item : output) {
                result.add(RankingItemDto.builder()
                        .symbol(str(item, "stck_shrn_iscd"))
                        .name(str(item, "hts_kor_isnm"))
                        .price(str(item, "stck_prpr"))
                        .change(str(item, "prdy_vrss"))
                        .changePercent(str(item, "prdy_ctrt"))
                        .volume(str(item, "acml_vol"))
                        .build());
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
                endpoint = "/most-active";
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
