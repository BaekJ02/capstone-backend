package capstone.controller;

import capstone.dto.PortfolioChartDto;
import capstone.service.PortfolioSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioSnapshotService portfolioSnapshotService;

    @GetMapping("/chart")
    public ResponseEntity<PortfolioChartDto> getPortfolioChart(
            @RequestParam(defaultValue = "30") int days) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        PortfolioChartDto chart = portfolioSnapshotService.getPortfolioChart(userId, days);
        return ResponseEntity.ok(chart);
    }
}
