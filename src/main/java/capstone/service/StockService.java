package capstone.service;

import capstone.dto.StockPriceDto;
import capstone.dto.StockSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import capstone.dto.ChartDataDto;

@Service
@RequiredArgsConstructor
public class StockService {

    private final RestTemplate restTemplate;
    private final KisAuthService kisAuthService;

    @Value("${kis.api.url}")
    private String baseUrl;

    // 국내 주식 현재가
    public StockPriceDto getDomesticStockPrice(String symbol) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?fid_cond_mrkt_div_code=J"
                + "&fid_input_iscd=" + symbol;

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKeyFromProperties());
        headers.set("appsecret", appSecretFromProperties());
        headers.set("tr_id", "FHKST01010100");
        headers.set("custtype", "P");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

        Map<String, String> output = (Map<String, String>) response.get("output");

        StockPriceDto dto = new StockPriceDto();
        dto.setSymbol(symbol);
        dto.setPrice(output.get("stck_prpr"));        // 현재가
        dto.setChange(output.get("prdy_vrss"));       // 전일대비
        dto.setChangePercent(output.get("prdy_ctrt")); // 등락률

        return dto;
    }

    @Value("${kis.api.key}")
    private String appKey;

    @Value("${kis.api.secret}")
    private String appSecret;

    private String appKeyFromProperties() { return appKey; }
    private String appSecretFromProperties() { return appSecret; }

    // 미국 주식 현재가
    public StockPriceDto getOverseasStockPrice(String symbol, String exchange) {
        String url = baseUrl + "/uapi/overseas-price/v1/quotations/price"
                + "?AUTH="
                + "&EXCD=" + exchange
                + "&SYMB=" + symbol;

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "HHDFS00000300");
        headers.set("custtype", "P");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

        Map<String, String> output = (Map<String, String>) response.get("output");

        StockPriceDto dto = new StockPriceDto();
        dto.setSymbol(symbol);
        dto.setPrice(output.get("last"));
        dto.setChange(output.get("diff"));
        dto.setChangePercent(output.get("rate"));

        return dto;
    }

    // 국내 주식 종목 검색
    public List<StockSearchDto> searchDomesticStock(String keyword) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/search-stock-info"
                + "?PRDT_TYPE_CD=300"
                + "&PDNO=" + keyword;

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "CTPF1002R");
        headers.set("custtype", "P");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

        List<Map<String, String>> output = (List<Map<String, String>>) response.get("output");

        List<StockSearchDto> result = new ArrayList<>();
        if (output != null) {
            for (Map<String, String> item : output) {
                StockSearchDto dto = new StockSearchDto();
                dto.setSymbol(item.get("pdno"));
                dto.setName(item.get("prdt_abrv_name"));
                dto.setMarket(item.get("prdt_type_cd"));
                result.add(dto);
            }
        }
        return result;
    }

    // 국내 주식 차트 데이터 (일/주/월/년)
    public List<ChartDataDto> getDomesticChartData(String symbol, String period) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"
                + "?fid_cond_mrkt_div_code=J"
                + "&fid_input_iscd=" + symbol
                + "&fid_input_date_1=19000101"
                + "&fid_input_date_2=99991231"
                + "&fid_period_div_code=" + period
                + "&fid_org_adj_prc=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST03010100");
        headers.set("custtype", "P");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

        List<Map<String, String>> output = (List<Map<String, String>>) response.get("output2");

        List<ChartDataDto> result = new ArrayList<>();
        if (output != null) {
            for (Map<String, String> item : output) {
                ChartDataDto dto = new ChartDataDto();
                dto.setDate(item.get("stck_bsop_date"));   // 날짜
                dto.setOpen(item.get("stck_oprc"));         // 시가
                dto.setHigh(item.get("stck_hgpr"));         // 고가
                dto.setLow(item.get("stck_lwpr"));          // 저가
                dto.setClose(item.get("stck_clpr"));        // 종가
                dto.setVolume(item.get("acml_vol"));        // 거래량
                result.add(dto);
            }
        }
        return result;
    }

    // 미국 주식 차트 데이터
    public List<ChartDataDto> getOverseasChartData(String symbol, String exchange, String period) {
        String url = baseUrl + "/uapi/overseas-price/v1/quotations/dailyprice"
                + "?AUTH="
                + "&EXCD=" + exchange
                + "&SYMB=" + symbol
                + "&GUBN=" + period
                + "&BYMD="
                + "&MODP=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "HHDFS76240000");
        headers.set("custtype", "P");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

        List<Map<String, String>> output = (List<Map<String, String>>) response.get("output2");

        List<ChartDataDto> result = new ArrayList<>();
        if (output != null) {
            for (Map<String, String> item : output) {
                ChartDataDto dto = new ChartDataDto();
                dto.setDate(item.get("xymd"));    // 날짜
                dto.setOpen(item.get("open"));    // 시가
                dto.setHigh(item.get("high"));    // 고가
                dto.setLow(item.get("low"));      // 저가
                dto.setClose(item.get("clos"));   // 종가
                dto.setVolume(item.get("tvol"));  // 거래량
                result.add(dto);
            }
        }
        return result;
    }


}