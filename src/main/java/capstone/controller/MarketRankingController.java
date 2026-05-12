package capstone.controller;

import capstone.dto.RankingItemDto;
import capstone.service.MarketRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketRankingController {

    private final MarketRankingService marketRankingService;

    @GetMapping("/domestic/ranking")
    public List<RankingItemDto> getDomesticRanking(
            @RequestParam(defaultValue = "RISE") String type) {
        return marketRankingService.getDomesticRanking(type);
    }

    @GetMapping("/overseas/ranking")
    public List<RankingItemDto> getOverseasRanking(
            @RequestParam(defaultValue = "RISE") String type) {
        return marketRankingService.getOverseasRanking(type);
    }
}
