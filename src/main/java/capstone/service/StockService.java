package capstone.service;

import capstone.dto.StockDetailDto;
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
import java.util.LinkedHashMap;

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

    // 국내 주식 분봉 데이터
    public List<ChartDataDto> getDomesticMinuteData(String symbol, String timeUnit) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice"
                + "?fid_etc_cls_code="
                + "&fid_cond_mrkt_div_code=J"
                + "&fid_input_iscd=" + symbol
                + "&fid_input_hour_1=160000"
                + "&fid_pw_data_incu_yn=Y"
                + "&fid_time_dvsn=" + timeUnit;

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST03010200");
        headers.set("custtype", "P");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

        List<Map<String, String>> output = (List<Map<String, String>>) response.get("output2");

        List<ChartDataDto> result = new ArrayList<>();
        if (output != null) {
            for (Map<String, String> item : output) {
                ChartDataDto dto = new ChartDataDto();
                dto.setDate(item.get("stck_cntg_hour"));  // 시간
                dto.setOpen(item.get("stck_oprc"));        // 시가
                dto.setHigh(item.get("stck_hgpr"));        // 고가
                dto.setLow(item.get("stck_lwpr"));         // 저가
                dto.setClose(item.get("stck_prpr"));       // 현재가
                dto.setVolume(item.get("cntg_vol"));       // 거래량
                result.add(dto);
            }
        }
        return result;
    }
    // 미국 주식 분봉 데이터
    public List<ChartDataDto> getOverseasMinuteData(String symbol, String exchange, String timeUnit) {
        String url = baseUrl + "/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice"
                + "?AUTH="
                + "&EXCD=" + exchange
                + "&SYMB=" + symbol
                + "&NMIN=" + timeUnit
                + "&PINC=1"
                + "&NEXT="
                + "&NREC=120"
                + "&FILL="
                + "&KEYB=";

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "HHDFS76950200");
        headers.set("custtype", "P");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();

        List<Map<String, String>> output = (List<Map<String, String>>) response.get("output2");

        List<ChartDataDto> result = new ArrayList<>();
        if (output != null) {
            for (Map<String, String> item : output) {
                ChartDataDto dto = new ChartDataDto();
                dto.setDate(item.get("kymd") + item.get("khms")); // 날짜+시간
                dto.setOpen(item.get("open"));
                dto.setHigh(item.get("high"));
                dto.setLow(item.get("low"));
                dto.setClose(item.get("last"));
                dto.setVolume(item.get("evol"));
                result.add(dto);
            }
        }
        return result;
    }

    // 국내 주식 상세정보
    public StockDetailDto getDomesticStockDetail(String symbol) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?fid_cond_mrkt_div_code=J"
                + "&fid_input_iscd=" + symbol;

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST01010100");
        headers.set("custtype", "P");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<String, Object> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();
        Map<String, String> output = (Map<String, String>) response.get("output");

        StockDetailDto dto = new StockDetailDto();
        dto.setSymbol(symbol);
        dto.setMarket(output.get("rprs_mrkt_kor_name"));
        dto.setPrice(output.get("stck_prpr"));
        dto.setChange(output.get("prdy_vrss"));
        dto.setChangePercent(output.get("prdy_ctrt"));
        dto.setMarketCap(output.get("hts_avls"));
        dto.setPer(output.get("per"));
        dto.setEps(output.get("eps"));
        dto.setPbr(output.get("pbr"));
        dto.setBps(output.get("bps"));
        dto.setHigh52(output.get("w52_hgpr"));
        dto.setLow52(output.get("w52_lwpr"));
        dto.setVolume(output.get("acml_vol"));
        dto.setTradingValue(output.get("acml_tr_pbmn"));

        return dto;
    }

    // 미국 주식 상세정보
    public StockDetailDto getOverseasStockDetail(String symbol, String exchange) {
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

        output.forEach((k, v) -> System.out.println(k + " = " + v));

        StockDetailDto dto = new StockDetailDto();
        dto.setSymbol(symbol);
        dto.setPrice(output.get("last"));
        dto.setChange(output.get("diff"));
        dto.setChangePercent(output.get("rate"));
        dto.setHigh52(output.get("h52p"));
        dto.setLow52(output.get("l52p"));
        dto.setVolume(output.get("tvol"));
        dto.setPer(output.get("per"));
        dto.setEps(output.get("eps"));

        String url2 = baseUrl + "/uapi/overseas-price/v1/quotations/search-info"
                + "?EXCD=" + exchange
                + "&SYMB=" + symbol;

        HttpHeaders headers2 = new HttpHeaders();
        headers2.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers2.set("appkey", appKey);
        headers2.set("appsecret", appSecret);
        headers2.set("tr_id", "HHDFS76240000");
        headers2.set("custtype", "P");

        HttpEntity<Void> request2 = new HttpEntity<>(headers2);
        Map<String, Object> response2 = restTemplate.exchange(url2, HttpMethod.GET, request2, Map.class).getBody();
        Map<String, String> output2 = (Map<String, String>) response2.get("output2");

        if (output2 != null) {
            output2.forEach((k, v) -> System.out.println(k + " = " + v));
        }

        String url3 = baseUrl + "/uapi/overseas-price/v1/quotations/search-info"
                + "?PRDT_TYPE_CD=512"
                + "&PDNO=" + symbol;

        HttpHeaders headers3 = new HttpHeaders();
        headers3.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers3.set("appkey", appKey);
        headers3.set("appsecret", appSecret);
        headers3.set("tr_id", "HHDFS76200200");
        headers3.set("custtype", "P");

        HttpEntity<Void> request3 = new HttpEntity<>(headers3);
        Map<String, Object> response3 = restTemplate.exchange(url3, HttpMethod.GET, request3, Map.class).getBody();
        Map<String, String> output3 = (Map<String, String>) response3.get("output");

        if (output3 != null) {
            output3.forEach((k, v) -> System.out.println(k + " = " + v));
        }

        return dto;
    }

    // 미국 주식 연봉 데이터 (월봉 데이터로 변환)
    public List<ChartDataDto> getOverseasYearlyData(String symbol, String exchange) {
        // 월봉 데이터 가져오기
        List<ChartDataDto> monthlyData = getOverseasChartData(symbol, exchange, "2");

        // 연도별로 그룹핑
        Map<String, List<ChartDataDto>> groupedByYear = new LinkedHashMap<>();
        for (ChartDataDto item : monthlyData) {
            String year = item.getDate().substring(0, 4); // 앞 4자리가 연도
            groupedByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(item);
        }

        // 연봉으로 변환
        List<ChartDataDto> result = new ArrayList<>();
        for (Map.Entry<String, List<ChartDataDto>> entry : groupedByYear.entrySet()) {
            List<ChartDataDto> monthList = entry.getValue();

            String open = monthList.get(monthList.size() - 1).getOpen();   // 첫달 시가
            String close = monthList.get(0).getClose();                     // 마지막달 종가
            String high = monthList.stream()
                    .map(d -> Double.parseDouble(d.getHigh()))
                    .max(Double::compareTo)
                    .map(String::valueOf)
                    .orElse("0");
            String low = monthList.stream()
                    .map(d -> Double.parseDouble(d.getLow()))
                    .min(Double::compareTo)
                    .map(String::valueOf)
                    .orElse("0");
            long totalVolume = monthList.stream()
                    .mapToLong(d -> Long.parseLong(d.getVolume()))
                    .sum();

            ChartDataDto dto = new ChartDataDto();
            dto.setDate(entry.getKey());
            dto.setOpen(open);
            dto.setHigh(high);
            dto.setLow(low);
            dto.setClose(close);
            dto.setVolume(String.valueOf(totalVolume));
            result.add(dto);
        }
        return result;
    }

}