package capstone.controller;

import capstone.dto.MarketNewsDto;
import capstone.service.MarketNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market/news")
@RequiredArgsConstructor
public class MarketNewsController {

    private final MarketNewsService marketNewsService;

    @GetMapping("/overseas")
    public ResponseEntity<MarketNewsDto> getOverseasNews() {
        MarketNewsDto news = marketNewsService.getOverseasNews();
        if (news == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(news);
    }

    @GetMapping("/domestic")
    public ResponseEntity<MarketNewsDto> getDomesticNews() {
        MarketNewsDto news = marketNewsService.getDomesticNews();
        if (news == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(news);
    }
}
