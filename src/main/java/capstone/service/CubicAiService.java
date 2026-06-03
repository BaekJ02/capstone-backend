package capstone.service;

import capstone.domain.CubicCellLog;
import capstone.dto.ChartDataDto;
import capstone.dto.CubicAnalyzeRequestDto;
import capstone.dto.CubicAnalyzeRequestDto.OhlcvRowDto;
import capstone.dto.CubicAnalyzeResponseDto;
import capstone.repository.CubicCellLogRepository;
import capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CubicAiService {

    private final WebClient webClient;
    private final StockService stockService;
    private final CubicCellLogRepository cubicCellLogRepository;
    private final UserRepository userRepository;

    @Value("${cubic.ai.url}")
    private String cubicAiUrl;

    private static final Set<String> OVERSEAS = Set.of("NASDAQ", "NYSE", "AMEX", "OTHER");

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
