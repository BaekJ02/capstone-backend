package capstone.controller;

import capstone.domain.Holding;
import capstone.dto.AiChatRequestDto;
import capstone.dto.AiChatResponseDto;
import capstone.dto.CubicAnalyzeResponseDto;
import capstone.repository.HoldingRepository;
import capstone.service.AiService;
import capstone.service.CubicAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final CubicAiService cubicAiService;
    private final HoldingRepository holdingRepository;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDto> chat(@RequestBody AiChatRequestDto request) {
        String reply = aiService.chat(request.getMessage(), request.getHistory());
        return ResponseEntity.ok(new AiChatResponseDto(reply));
    }

    @PostMapping("/analyze/holdings")
    public ResponseEntity<AiChatResponseDto> analyzeHoldings() {
        try {
            String holdingsText = buildHoldingsText();
            if (holdingsText == null) {
                return ResponseEntity.ok(new AiChatResponseDto("보유 종목이 없습니다. 먼저 주식을 매수해주세요."));
            }
            return ResponseEntity.ok(new AiChatResponseDto(aiService.analyzeHoldings(holdingsText)));
        } catch (Exception e) {
            log.error("종목 분석 실패: {}", e.getMessage());
            return ResponseEntity.ok(new AiChatResponseDto("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    @PostMapping("/analyze/portfolio")
    public ResponseEntity<AiChatResponseDto> analyzePortfolio() {
        try {
            String holdingsText = buildHoldingsText();
            if (holdingsText == null) {
                return ResponseEntity.ok(new AiChatResponseDto("보유 종목이 없습니다. 먼저 주식을 매수해주세요."));
            }
            return ResponseEntity.ok(new AiChatResponseDto(aiService.analyzePortfolio(holdingsText)));
        } catch (Exception e) {
            log.error("포트폴리오 분석 실패: {}", e.getMessage());
            return ResponseEntity.ok(new AiChatResponseDto("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    @PostMapping("/analyze/recommend")
    public ResponseEntity<AiChatResponseDto> recommendStocks() {
        try {
            String holdingsText = buildHoldingsText();
            if (holdingsText == null) {
                return ResponseEntity.ok(new AiChatResponseDto("보유 종목이 없습니다. 먼저 주식을 매수해주세요."));
            }
            return ResponseEntity.ok(new AiChatResponseDto(aiService.recommendStocks(holdingsText)));
        } catch (Exception e) {
            log.error("종목 추천 실패: {}", e.getMessage());
            return ResponseEntity.ok(new AiChatResponseDto("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    @GetMapping("/cubic/health")
    public ResponseEntity<Map<String, Object>> cubicHealth() {
        return ResponseEntity.ok(Map.of("healthy", cubicAiService.isHealthy()));
    }

    @GetMapping("/cubic/latest/{symbol}")
    public ResponseEntity<?> cubicLatest(@PathVariable String symbol) {
        CubicAnalyzeResponseDto result = cubicAiService.getLatestBySymbol(symbol);
        if (result == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cubic/batch")
    public ResponseEntity<Map<String, Object>> cubicBatch(
            @RequestBody List<String> symbols) {
        Map<String, Object> result = new java.util.HashMap<>();
        for (String symbol : symbols) {
            CubicAnalyzeResponseDto dto = cubicAiService.getLatestBySymbol(symbol);
            if (dto != null) {
                result.put(symbol, Map.of(
                    "action", dto.getAction(),
                    "cubicScore", dto.getCubicScore() != null ? dto.getCubicScore() : 50,
                    "cellNum", dto.getCell() != null ? dto.getCell().getCellNum() : -1
                ));
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cubic/analyze")
    public ResponseEntity<CubicAnalyzeResponseDto> cubicAnalyze(
            @RequestBody Map<String, String> body) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String symbol = body.get("symbol");
        String market = body.getOrDefault("market", "KOSPI");
        CubicAnalyzeResponseDto result = cubicAiService.analyze(symbol, market, userId);
        return ResponseEntity.ok(result);
    }

    private static final Set<String> OVERSEAS = Set.of("NASDAQ", "NYSE", "AMEX", "OTHER");

    private String buildHoldingsText() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        if (holdings.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        for (Holding h : holdings) {
            boolean isOverseas = OVERSEAS.contains(h.getMarket());
            if (isOverseas) {
                sb.append(String.format("%s(%s) | %s | 보유수량: %d주 | 평균매수가: $%.2f%n",
                    h.getName(), h.getSymbol(), h.getMarket(), h.getQuantity(), h.getAvgPrice()));
            } else {
                sb.append(String.format("%s(%s) | %s | 보유수량: %d주 | 평균매수가: %,.0f원%n",
                    h.getName(), h.getSymbol(), h.getMarket(), h.getQuantity(), h.getAvgPrice()));
            }
        }
        return sb.toString();
    }
}
