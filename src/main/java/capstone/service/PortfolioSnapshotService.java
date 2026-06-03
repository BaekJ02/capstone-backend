package capstone.service;

import capstone.domain.Holding;
import capstone.domain.PortfolioSnapshot;
import capstone.domain.User;
import capstone.dto.ChartDataDto;
import capstone.dto.PortfolioChartDto;
import capstone.repository.HoldingRepository;
import capstone.repository.PortfolioSnapshotRepository;
import capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioSnapshotService {

    private final PortfolioSnapshotRepository snapshotRepository;
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final StockService stockService;

    private static final Set<String> OVERSEAS = Set.of("NASDAQ", "NYSE", "AMEX", "OTHER");
    private static final double DEFAULT_EXCHANGE_RATE = 1380.0;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void saveSnapshotsForAllUsers() {
        log.info("포트폴리오 스냅샷 저장 시작: {}", LocalDateTime.now());
        List<User> users = userRepository.findAll();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        for (User user : users) {
            try {
                if (snapshotRepository.findByUserIdAndDate(user.getId(), yesterday).isPresent()) {
                    continue;
                }
                double totalValue = calculateTotalValue(user.getId(), yesterday);
                PortfolioSnapshot snapshot = new PortfolioSnapshot();
                snapshot.setUser(user);
                snapshot.setDate(yesterday);
                snapshot.setTotalValue(totalValue);
                snapshotRepository.save(snapshot);

                LocalDate cutoff = yesterday.minusDays(30);
                snapshotRepository.deleteOldSnapshots(user.getId(), cutoff);

                log.info("스냅샷 저장 완료 - userId: {}, date: {}, value: {}", user.getId(), yesterday, totalValue);
            } catch (Exception e) {
                log.error("스냅샷 저장 실패 - userId: {}, error: {}", user.getId(), e.getMessage());
            }
        }
    }

    public PortfolioChartDto getPortfolioChart(Long userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days);

        List<PortfolioSnapshot> snapshots = snapshotRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, today);

        List<PortfolioChartDto.DataPoint> dataPoints = snapshots.stream()
                .map(s -> new PortfolioChartDto.DataPoint(
                        s.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        s.getTotalValue()))
                .collect(Collectors.toList());

        try {
            double todayValue = calculateTotalValue(userId, today);
            dataPoints.add(new PortfolioChartDto.DataPoint(
                    today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    todayValue));
        } catch (Exception e) {
            log.error("오늘 포트폴리오 계산 실패: {}", e.getMessage());
        }

        if (dataPoints.isEmpty()) {
            return new PortfolioChartDto(dataPoints, 0, 0, 0, 0);
        }

        double startValue = dataPoints.get(0).getValue();
        double currentValue = dataPoints.get(dataPoints.size() - 1).getValue();
        double changeAmount = currentValue - startValue;
        double changeRate = startValue > 0 ? (changeAmount / startValue) * 100 : 0;

        return new PortfolioChartDto(dataPoints, currentValue, startValue, changeAmount, changeRate);
    }

    public double calculateTotalValue(Long userId, LocalDate date) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        User user = userRepository.findById(userId).orElseThrow();

        double total = user.getBalance() != null ? user.getBalance() : 0.0;

        for (Holding h : holdings) {
            try {
                boolean isOverseas = OVERSEAS.contains(h.getMarket());
                double price;

                if (isOverseas) {
                    String exchange = switch (h.getMarket()) {
                        case "NYSE" -> "NYS";
                        case "AMEX" -> "AMS";
                        default -> "NAS";
                    };
                    List<ChartDataDto> chartData = stockService.getOverseasChartData(h.getSymbol(), exchange, "0");
                    price = getPriceForDate(chartData, date) * DEFAULT_EXCHANGE_RATE;
                } else {
                    List<ChartDataDto> chartData = stockService.getDomesticChartData(h.getSymbol(), "D");
                    price = getPriceForDate(chartData, date);
                }

                total += price * h.getQuantity();
            } catch (Exception e) {
                log.warn("종목 가격 조회 실패 - symbol: {}, error: {}", h.getSymbol(), e.getMessage());
                total += h.getAvgPrice() * h.getQuantity();
            }
        }

        return total;
    }

    private double getPriceForDate(List<ChartDataDto> chartData, LocalDate targetDate) {
        if (chartData == null || chartData.isEmpty()) return 0.0;

        String targetStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for (ChartDataDto d : chartData) {
            if (targetStr.equals(d.getDate())) {
                return parseDouble(d.getClose());
            }
        }

        ChartDataDto closest = null;
        for (ChartDataDto d : chartData) {
            if (d.getDate() != null && d.getDate().compareTo(targetStr) <= 0) {
                if (closest == null || d.getDate().compareTo(closest.getDate()) > 0) {
                    closest = d;
                }
            }
        }

        return closest != null ? parseDouble(closest.getClose()) : parseDouble(chartData.get(0).getClose());
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
