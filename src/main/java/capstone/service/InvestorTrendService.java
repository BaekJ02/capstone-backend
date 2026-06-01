package capstone.service;

import capstone.dto.InvestorTrendDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestorTrendService {

    private final RestTemplate restTemplate;
    private final KisAuthService kisAuthService;

    @Value("${kis.api.url}")
    private String kisBaseUrl;

    @Value("${kis.api.key}")
    private String appKey;

    @Value("${kis.api.secret}")
    private String appSecret;

    public List<InvestorTrendDto> getInvestorTrend(String symbol) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String url = UriComponentsBuilder
                .fromUriString(kisBaseUrl + "/uapi/domestic-stock/v1/quotations/investor-trade-by-stock-daily")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", symbol)
                .queryParam("FID_INPUT_DATE_1", today)
                .queryParam("FID_ORG_ADJ_PRC", "")
                .queryParam("FID_ETC_CLS_CODE", "1")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHPTJ04160001");
        headers.set("custtype", "P");

        log.info("KIS 투자자별 매매동향 호출 [symbol={}] URL: {}", symbol, url);

        Map<String, Object> response = restTemplate
                .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class)
                .getBody();

        if (response == null) return Collections.emptyList();

        List<Map<String, Object>> output2 = (List<Map<String, Object>>) response.get("output2");
        if (output2 == null || output2.isEmpty()) return Collections.emptyList();

        List<InvestorTrendDto> result = output2.stream()
                .limit(10)
                .map(item -> InvestorTrendDto.builder()
                        .date(str(item, "stck_bsop_date"))
                        .closePrice(str(item, "stck_clpr"))
                        .personalNet(str(item, "prsn_ntby_qty"))
                        .foreignNet(str(item, "frgn_ntby_qty"))
                        .institutionNet(str(item, "orgn_ntby_qty"))
                        .build())
                .collect(Collectors.toList());

        result.sort(Comparator.comparing(InvestorTrendDto::getDate));
        return result;
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
