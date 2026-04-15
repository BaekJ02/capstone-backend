package capstone.controller;

import capstone.dto.StockPriceDto;
import capstone.dto.StockSearchDto;
import capstone.service.StockSearchService;
import capstone.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;
    private final StockSearchService stockSearchService;

    // 국내 주식
    @GetMapping("/domestic/{symbol}")
    public StockPriceDto getDomesticStock(@PathVariable String symbol) {
        return stockService.getDomesticStockPrice(symbol);
    }

    // 미국 주식
    @GetMapping("/overseas/{symbol}")
    public StockPriceDto getOverseasStock(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "NAS") String exchange) {
        return stockService.getOverseasStockPrice(symbol, exchange);
    }

    // 국내 주식 종목 검색
    @GetMapping("/search/domestic")
    public List<StockSearchDto> searchDomesticStock(@RequestParam String keyword) {
        return stockService.searchDomesticStock(keyword);
    }

    // 종목 검색
    @GetMapping("/search")
    public List<StockSearchDto> searchStock(@RequestParam String keyword) {
        return stockSearchService.search(keyword);
    }
}
