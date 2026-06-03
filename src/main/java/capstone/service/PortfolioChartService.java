package capstone.service;

import capstone.domain.Holding;
import capstone.domain.User;
import capstone.dto.ChartDataDto;
import capstone.dto.PortfolioChartDto;
import capstone.repository.HoldingRepository;
import capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioChartService {

    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final ExchangeRateService exchangeRateService;

    private static final Set<String> OVERSEAS_MARKETS = Set.of("NAS", "NYS", "AMS", "나스닥", "뉴욕");

    public PortfolioChartDto getPortfolioChart(Long userId, int days) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        double exchangeRate = exchangeRateService.getCurrentRate();

        // 날짜 범위 (최근 days 거래일 커버를 위해 days*2 분량 조회 후 슬라이스)
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays((long) days * 2);

        // 날짜 → 총평가금액 맵 (현금은 상수 취급)
        TreeMap<String, Double> dateValueMap = new TreeMap<>();

        // 보유 종목별 일봉 데이터 수집
        for (Holding holding : holdings) {
            try {
                List<ChartDataDto> chartData = fetchDailyChart(holding);
                if (chartData == null || chartData.isEmpty()) continue;

                boolean isOverseas = OVERSEAS_MARKETS.contains(holding.getMarket());
                double qty = holding.getQuantity();

                for (ChartDataDto bar : chartData) {
                    String date = normalizeDate(bar.getDate());
                    if (date == null) continue;
                    LocalDate barDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    if (barDate.isBefore(from) || barDate.isAfter(today)) continue;

                    double close = parseDouble(bar.getClose());
                    double stockValue = close * qty;
                    if (isOverseas) stockValue *= exchangeRate;

                    dateValueMap.merge(date, stockValue, Double::sum);
                }
            } catch (Exception e) {
                log.warn("포트폴리오 차트 종목 조회 실패 ({}): {}", holding.getSymbol(), e.getMessage());
            }
        }

        // 현금 잔고 합산 — 날짜별로 동일하게 추가
        double cash = user.getBalance() + user.getDollarBalance() * exchangeRate;
        for (String date : dateValueMap.keySet()) {
            dateValueMap.merge(date, cash, Double::sum);
        }

        // 보유 종목이 없거나 데이터가 없으면 현금만으로 today 1개 포인트 반환
        if (dateValueMap.isEmpty()) {
            String todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            dateValueMap.put(todayStr, cash);
        }

        // 주말/공휴일 결측 날짜 처리: 직전 거래일 값으로 전파 (forward fill)
        dateValueMap = forwardFill(dateValueMap, from, today);

        // 최근 days일치만 추출
        List<PortfolioChartDto.DataPoint> points = new ArrayList<>();
        List<String> sortedDates = new ArrayList<>(dateValueMap.keySet());
        int startIdx = Math.max(0, sortedDates.size() - days);
        for (int i = startIdx; i < sortedDates.size(); i++) {
            String date = sortedDates.get(i);
            // "yyyyMMdd" → "yyyy-MM-dd"
            String formatted = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
            points.add(new PortfolioChartDto.DataPoint(formatted, Math.round(dateValueMap.get(date) * 10) / 10.0));
        }

        double startTotal = points.isEmpty() ? cash : points.get(0).getValue();
        double currentTotal = points.isEmpty() ? cash : points.get(points.size() - 1).getValue();
        double changeAmount = currentTotal - startTotal;
        double changeRate = startTotal == 0 ? 0 : (changeAmount / startTotal) * 100;

        return new PortfolioChartDto(points, currentTotal, startTotal, changeAmount, changeRate);
    }

    private List<ChartDataDto> fetchDailyChart(Holding holding) {
        String market = holding.getMarket();
        if (OVERSEAS_MARKETS.contains(market)) {
            String exchange = toKisExchange(market);
            return stockService.getOverseasChartData(holding.getSymbol(), exchange, "0");
        } else {
            return stockService.getDomesticChartData(holding.getSymbol(), "D");
        }
    }

    private String toKisExchange(String market) {
        return switch (market) {
            case "나스닥", "NAS" -> "NAS";
            case "뉴욕", "NYS" -> "NYS";
            case "AMS" -> "AMS";
            default -> "NAS";
        };
    }

    private String normalizeDate(String raw) {
        if (raw == null || raw.length() < 8) return null;
        // 이미 "yyyyMMdd" 형태이거나 앞 8자리만 사용
        return raw.substring(0, 8);
    }

    private double parseDouble(String s) {
        try {
            return (s == null || s.isBlank()) ? 0 : Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // from~to 범위의 모든 날짜를 순회하며 값이 없는 날은 직전 값으로 채움
    private TreeMap<String, Double> forwardFill(TreeMap<String, Double> map, LocalDate from, LocalDate to) {
        if (map.isEmpty()) return map;
        TreeMap<String, Double> filled = new TreeMap<>(map);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        Double lastVal = null;
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            String key = cur.format(fmt);
            if (filled.containsKey(key)) {
                lastVal = filled.get(key);
            } else if (lastVal != null) {
                filled.put(key, lastVal);
            }
            cur = cur.plusDays(1);
        }
        return filled;
    }
}
