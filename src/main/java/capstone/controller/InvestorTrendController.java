package capstone.controller;

import capstone.dto.InvestorTrendDto;
import capstone.service.InvestorTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class InvestorTrendController {

    private final InvestorTrendService investorTrendService;

    private static final Set<String> DOMESTIC_MARKETS = Set.of("KOSPI", "KOSDAQ", "ETF");

    @GetMapping("/investor/{symbol}")
    public List<InvestorTrendDto> getInvestorTrend(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "KOSPI") String market) {
        if (!DOMESTIC_MARKETS.contains(market.toUpperCase())) {
            return Collections.emptyList();
        }
        return investorTrendService.getInvestorTrend(symbol);
    }
}
