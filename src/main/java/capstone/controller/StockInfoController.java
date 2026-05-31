package capstone.controller;

import capstone.dto.StockInfoDto;
import capstone.service.StockInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockInfoController {

    private final StockInfoService stockInfoService;

    @GetMapping("/info/{symbol}")
    public StockInfoDto getStockInfo(
            @PathVariable String symbol,
            @RequestParam String market) {
        return stockInfoService.getStockInfo(symbol, market);
    }
}
