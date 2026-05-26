package capstone.controller;

import capstone.dto.MarketNewsDto;
import capstone.service.MarketNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketNewsController {

    private final MarketNewsService marketNewsService;

    @GetMapping("/news")
    public ResponseEntity<MarketNewsDto> getMarketNews() {
        MarketNewsDto news = marketNewsService.getNews();
        if (news == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(news);
    }
}
