package capstone.service;

import capstone.domain.CubicCellLog;
import capstone.dto.ChartDataDto;
import capstone.dto.CubicAnalyzeRequestDto;
import capstone.dto.CubicAnalyzeRequestDto.OhlcvRowDto;
import capstone.dto.CubicAnalyzeResponseDto;
import capstone.dto.RankingItemDto;
import capstone.repository.CubicCellLogRepository;
import capstone.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CubicAiService {

    private final WebClient webClient;
    private final StockService stockService;
    private final CubicCellLogRepository cubicCellLogRepository;
    private final UserRepository userRepository;
    private final MarketRankingService marketRankingService;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @Value("${cubic.ai.url}")
    private String cubicAiUrl;

    private static final Set<String> OVERSEAS = Set.of("NASDAQ", "NYSE", "AMEX", "OTHER");

    @PostConstruct
    public void initCubicAnalysis() {
        executor.submit(() -> {
            try {
                Thread.sleep(15000);
                LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
                boolean recentlyAnalyzed = cubicCellLogRepository
                    .findTopByOrderByAnalyzedAtDesc()
                    .map(log -> log.getAnalyzedAt() != null && log.getAnalyzedAt().isAfter(twoHoursAgo))
                    .orElse(false);

                if (recentlyAnalyzed) {
                    log.info("=== Cubic AI 초기 분석 스킵 (2시간 이내 분석 기록 존재) ===");
                    return;
                }

                log.info("=== Cubic AI 초기 분석 시작 ===");
                analyzeTopDomestic();
                analyzeTopOverseas();
                log.info("=== Cubic AI 초기 분석 완료 ===");
            } catch (Exception e) {
                log.error("Cubic 초기 분석 오류: {}", e.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 0 */4 * * *")
    public void scheduledCubicAnalysis() {
        log.info("=== Cubic AI 정기 분석 시작 ===");
        executor.submit(this::analyzeTopDomestic);
        executor.submit(this::analyzeTopOverseas);
    }

    public CubicAnalyzeResponseDto analyze(String symbol, String market, Long userId) {
        List<ChartDataDto> chartData = fetchOhlcv(symbol, market);
        if (chartData.size() < 60) {
            throw new IllegalStateException("데이터 부족: " + chartData.size() + "일 (최소 60일 필요)");
        }

        List<OhlcvRowDto> ohlcvRows = chartData.stream().map(c -> {
            OhlcvRowDto row = new OhlcvRowDto();
            row.setDate(c.getDate());
            row.setOpen(parseDouble(c.getOpen()));
            row.setHigh(parseDouble(c.getHigh()));
            row.setLow(parseDouble(c.getLow()));
            row.setClose(parseDouble(c.getClose()));
            row.setVolume(parseDouble(c.getVolume()));
            return row;
        }).toList();

        CubicAnalyzeRequestDto request = new CubicAnalyzeRequestDto();
        request.setSymbol(symbol);
        request.setOhlcv(ohlcvRows);

        CubicAnalyzeResponseDto response;
        try {
            response = webClient.post()
                    .uri(cubicAiUrl + "/analyze/signal")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CubicAnalyzeResponseDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Python AI 서버 오류: {}", e.getResponseBodyAsString());
            throw new RuntimeException("AI 분석 실패: " + e.getMessage());
        }

        if (response != null) {
            saveCellLog(response, userId);
        }

        return response;
    }

    public boolean isHealthy() {
        try {
            Map<?, ?> result = webClient.get()
                    .uri(cubicAiUrl + "/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(3))
                    .block();
            return result != null && "ok".equals(result.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    public CubicAnalyzeResponseDto getLatestBySymbol(String symbol) {
        return cubicCellLogRepository.findTopBySymbolOrderByAnalyzedAtDesc(symbol)
            .map(log -> {
                CubicAnalyzeResponseDto dto = new CubicAnalyzeResponseDto();
                dto.setSymbol(log.getSymbol());
                dto.setAction(log.getAction());
                dto.setActionCode(log.getActionCode());
                dto.setCubicScore(log.getCubicScore());
                CubicAnalyzeResponseDto.CellDto cell = new CubicAnalyzeResponseDto.CellDto();
                cell.setCellNum(log.getCellNum());
                dto.setCell(cell);
                return dto;
            })
            .orElse(null);
    }

    public CubicAnalyzeResponseDto analyzeAndCache(String symbol, String market, Long userId) {
        CubicAnalyzeResponseDto cached = getLatestBySymbol(symbol);
        if (cached != null) return cached;
        return analyze(symbol, market, userId);
    }

    public void analyzeTop(String symbol, String market) {
        try {
            analyze(symbol, market, null);
            log.info("Cubic 분석 완료: {}", symbol);
        } catch (Exception e) {
            log.warn("Cubic 분석 실패: {} - {}", symbol, e.getMessage());
        }
    }

    private void analyzeTopDomestic() {
        if (!isHealthy()) {
            log.warn("Cubic AI 서버 미응답 - 국내 상위 종목 분석 스킵");
            return;
        }
        try {
            List<RankingItemDto> top = marketRankingService.getDomesticRanking("VOLUME");
            List<RankingItemDto> top20 = top.subList(0, Math.min(20, top.size()));
            log.info("국내 상위 {}종목 Cubic 분석 시작", top20.size());
            for (RankingItemDto item : top20) {
                try {
                    analyzeTop(item.getSymbol(), "KOSPI");
                    Thread.sleep(800);
                } catch (Exception e) {
                    log.warn("국내 종목 분석 실패 [{}]: {}", item.getSymbol(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("국내 상위 종목 분석 오류: {}", e.getMessage());
        }
    }

    private void analyzeTopOverseas() {
        if (!isHealthy()) {
            log.warn("Cubic AI 서버 미응답 - 미국 상위 종목 분석 스킵");
            return;
        }
        try {
            List<RankingItemDto> top = marketRankingService.getOverseasRanking("VOLUME");
            List<RankingItemDto> top20 = top.subList(0, Math.min(20, top.size()));
            log.info("미국 상위 {}종목 Cubic 분석 시작", top20.size());
            for (RankingItemDto item : top20) {
                try {
                    analyzeTop(item.getSymbol(), "NASDAQ");
                    Thread.sleep(800);
                } catch (Exception e) {
                    log.warn("미국 종목 분석 실패 [{}]: {}", item.getSymbol(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("미국 상위 종목 분석 오류: {}", e.getMessage());
        }
    }

    private List<ChartDataDto> fetchOhlcv(String symbol, String market) {
        if (OVERSEAS.contains(market)) {
            String exchange = switch (market) {
                case "NYSE" -> "NYS";
                case "AMEX" -> "AMS";
                default -> "NAS";
            };
            return stockService.getOverseasChartData(symbol, exchange, "D");
        }
        return stockService.getDomesticChartData(symbol, "D");
    }

    private void saveCellLog(CubicAnalyzeResponseDto resp, Long userId) {
        CubicCellLog cellLog = new CubicCellLog();
        cellLog.setSymbol(resp.getSymbol());
        cellLog.setCellX(resp.getRegimeRaw());
        cellLog.setCellY(resp.getRiskRaw());
        cellLog.setCellZ(resp.getMomentumRaw());
        cellLog.setCellNum(resp.getCell().getCellNum());
        cellLog.setAction(resp.getAction());
        cellLog.setActionCode(resp.getActionCode());
        cellLog.setCubicScore(resp.getCubicScore());

        if (userId != null) {
            userRepository.findById(userId).ifPresent(cellLog::setUser);
        }
        cubicCellLogRepository.save(cellLog);
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        return Double.parseDouble(value.replace(",", ""));
    }
}
