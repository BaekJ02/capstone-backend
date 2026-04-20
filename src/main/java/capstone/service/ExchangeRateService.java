package capstone.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate;

    @Value("${koreaexim.api.key}")
    private String apiKey;

    private volatile double cachedRate = 1300.0;

    public ExchangeRateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public double getCurrentRate() {
        return cachedRate;
    }

    @Scheduled(fixedRate = 3600000)
    public void refreshRate() {
        Double rate = fetchRate();
        if (rate != null) {
            cachedRate = rate;
            log.info("환율 갱신: {} KRW/USD", cachedRate);
        }
    }

    private Double fetchRate() {
        LocalDate date = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            String searchDate = date.minusDays(i).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            try {
                String url = "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON"
                        + "?authkey=" + apiKey
                        + "&searchdate=" + searchDate
                        + "&data=AP01";
                List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);
                if (response == null || response.isEmpty()) continue;
                for (Map<String, Object> item : response) {
                    if ("USD".equals(item.get("cur_unit"))) {
                        String rateStr = (String) item.get("deal_bas_r");
                        if (rateStr != null && !rateStr.isBlank()) {
                            return Double.parseDouble(rateStr.replace(",", ""));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("환율 조회 실패 ({}): {}", searchDate, e.getMessage());
            }
        }
        return null;
    }
}
